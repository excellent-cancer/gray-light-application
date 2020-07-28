package gray.light.book.job;

import gray.light.book.DocumentRepositoryVisitor;
import gray.light.book.job.step.BatchCloneRemoteRepositoryStep;
import gray.light.book.job.step.BatchUpdateDocumentRepositoriesStep;
import gray.light.book.job.step.UploadDocumentStep;
import gray.light.book.job.step.VisitDocumentRepositoryStep;
import gray.light.book.service.BookRepositoryCacheService;
import gray.light.book.service.BookService;
import gray.light.book.service.BookSourceService;
import gray.light.owner.entity.ProjectDetails;
import gray.light.owner.entity.ProjectStatus;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;
import java.util.function.Supplier;

/**
 * 从远程仓库克隆文档仓库，并且将其上传到文件服务器，最后更新数据库
 *
 * @author XyParaCrim
 */
@CommonsLog
public class UploadDocumentRepositoryJob extends QuartzJobBean {

    @Setter
    private Supplier<List<ProjectDetails>> initStatusProjectDetails;

    @Setter
    private BookService bookService;

    @Setter
    private BookSourceService bookSourceService;

    @Setter
    private BookRepositoryCacheService bookRepositoryCacheService;

    @Override
    protected void executeInternal(JobExecutionContext context) {

        List<ProjectDetails> emptyDocument = initStatusProjectDetails.get();

        // 克隆远程仓库到本地
        BatchCloneRemoteRepositoryStep.Result cloneResult = batchCloneRemoteRepository(emptyDocument);

        // 浏览仓库文件
        VisitDocumentRepositoryStep.Result visitResult = batchWalkGitTree(cloneResult.getSuccess());

        // 上传文件到文件服务器
        UploadDocumentStep.Result uploadResult = batchUploadDocument(visitResult.getVisitors());

        // 批量更新数据到数据库
        updateRepositories(uploadResult.getVisitors(), emptyDocument);
    }

    /**
     * 根据状态为空文档的仓库地址，克隆文档仓库到本地，如果其中有文档在克隆期间发生
     * 异常，则将文档状态设置为{@link ProjectStatus#INVALID}无效状态，并将其
     * 从文档迭代器中移除
     *
     * @param emptyDocument 状态为空的文档
     */
    private BatchCloneRemoteRepositoryStep.Result batchCloneRemoteRepository(List<ProjectDetails> emptyDocument) {
        BatchCloneRemoteRepositoryStep batchCloneRemoteRepository = new BatchCloneRemoteRepositoryStep(bookRepositoryCacheService);

        return batchCloneRemoteRepository.execute(emptyDocument);
    }

    /**
     * 遍历本地文档仓库，获取目录、章节等信息，并将其储存在{@link DocumentRepositoryVisitor}里返回。
     * 如果其中有遍历期间发生异常，则将文档状态设置为{@link ProjectStatus#INVALID}无效状态，并将其
     * 从文档迭代器中移除
     *
     * @param emptyDocument 状态为空的文档
     * @return 文档仓库遍历的结果
     */
    private VisitDocumentRepositoryStep.Result batchWalkGitTree(List<ProjectDetails> emptyDocument) {
        VisitDocumentRepositoryStep batchWalkGitTree = new VisitDocumentRepositoryStep(bookRepositoryCacheService);

        return batchWalkGitTree.execute(emptyDocument);
    }

    /**
     * 将文档仓库遍历的结果上传到文件服务器
     *
     * @param visitors 文件遍历器
     * @return 文档上传的结果
     */
    private UploadDocumentStep.Result batchUploadDocument(List<DocumentRepositoryVisitor> visitors) {
        UploadDocumentStep batchUploadDocument = new UploadDocumentStep(bookSourceService);

        return batchUploadDocument.execute(visitors);
    }

    /**
     * 更新文档状态
     *
     * @param visitors      文件遍历器
     * @param emptyDocument 文档
     */
    private void updateRepositories(List<DocumentRepositoryVisitor> visitors, List<ProjectDetails> emptyDocument) {
        BatchUpdateDocumentRepositoriesStep updateRepositories = new BatchUpdateDocumentRepositoriesStep(bookService);

        updateRepositories.execute(visitors, emptyDocument);
    }

}
