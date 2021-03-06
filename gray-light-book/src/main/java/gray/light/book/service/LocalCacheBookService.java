package gray.light.book.service;

import floor.repository.RepositoryDatabase;
import floor.repository.RepositoryOptions;
import gray.light.book.DocumentRepositoryVisitor;
import gray.light.owner.entity.ProjectDetails;
import gray.light.owner.entity.ProjectStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

/**
 * 定义有关于文档文件的上传、下载和更改
 *
 * @author XyParaCrim
 */
@CommonsLog
@RequiredArgsConstructor
public class LocalCacheBookService {

    private final RepositoryDatabase<Long, Long> repositoryDatabase;

    /**
     * 更新本地仓库。若长时间未获得操作权，则不修改文档状态，
     * 若在更新期间发生异常，则将文档状态修改为{@code DocumentStatus.INVALID}
     *
     * @param projectDetails 指定文档
     * @throws InterruptedException 当获取操作许可时，发生中断
     */
    public void updateRepository(@NonNull ProjectDetails projectDetails) throws InterruptedException {
        RepositoryOptions<Long, Long> options = repositoryDatabase.repositoryOptions(projectDetails.getOriginId());

        Optional<Long> writeStamp = options.writePermission();
        if (writeStamp.isPresent()) {
            try {
                // 比较版本差异

                if (options.hasUpdate()) {
                    options.updateLocal();
                    projectDetails.setStatus(ProjectStatus.PENDING);
                } else if (StringUtils.hasLength(projectDetails.getVersion()) &&
                        !options.equalsVersion(projectDetails.getVersion())) {
                    projectDetails.setStatus(ProjectStatus.PENDING);
                }
            } catch (GitAPIException | IOException e) {
                failureCache(projectDetails);
                log.error("Cannot update the repository: " + projectDetails.getOriginId(), e);
            } finally {
                options.cancelWritePermission(writeStamp.get());
            }
        } else {
            log.warn("Skip repository update since write permission is occupied");
        }
    }

    /**
     * 在一次fetch项目更新失败后，重新设置状态。若之前的状态为{@link ProjectStatus#WAIT_PERSISTENCE}时，
     * 需要将他设为{@link ProjectStatus#RETRY_PERSISTENCE}，其他为{@link ProjectStatus#FAILURE_CHECK}。
     * 目的是为了区别之前的版本是否可用
     *
     * @param projectDetails 项目详细
     */
    private void failureCache(ProjectDetails projectDetails) {
        if (projectDetails.getStatus() == ProjectStatus.WAIT_PERSISTENCE) {
            projectDetails.setStatus(ProjectStatus.RETRY_PERSISTENCE);
        } else {
            projectDetails.setStatus(ProjectStatus.FAILURE_CHECK);
        }
    }

    /**
     * 强制缓存文档仓库到本地，即使已存在的仓库与指定仓库一致，也会完全删除，
     * 再重新克隆
     *
     * @param document 指定文档
     */
    public void forceCacheRepository(@NonNull ProjectDetails document) {
        RepositoryOptions<Long, Long> options = repositoryDatabase.addRepositoryOptions(document.getOriginId(), document.getHttp());
        document.setVersion(options.version());
    }

    /**
     * 浏览指定文档仓库，并返回浏览结果
     *
     * @param document 指定文档
     * @return 浏览结果
     * @throws InterruptedException 当获取读权限时
     */
    public DocumentRepositoryVisitor visitRepository(ProjectDetails document) throws InterruptedException {
        // 获取文档仓库的使用权限
        RepositoryOptions<Long, Long> options = repositoryDatabase.repositoryOptions(document.getOriginId());
        Optional<Long> stamp = options.readPermission();

        if (stamp.isEmpty()) {
            return DocumentRepositoryVisitor.failedVisitor(document, new IllegalStateException("Failed to acquire document repository semaphore"));
        }

        try {
            DocumentRepositoryVisitor visitor;
            try {
                visitor = new DocumentRepositoryVisitor(document);
                Files.walkFileTree(options.getLocation().toPath(), visitor);

            } catch (IOException e) {
                return DocumentRepositoryVisitor.failedVisitor(document, e);
            }

            return visitor;
        } finally {
            options.cancelReadPermission(stamp.get());
        }
    }

    public boolean availableRepository(ProjectDetails document) {
        RepositoryOptions<Long, Long> options;
        return (options = repositoryDatabase.repositoryOptions(document.getOriginId())) != null &&
                Files.isDirectory(options.getLocation().toPath());
    }

}