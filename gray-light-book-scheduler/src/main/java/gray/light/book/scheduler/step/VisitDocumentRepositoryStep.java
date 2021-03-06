package gray.light.book.scheduler.step;

import gray.light.book.DocumentRepositoryVisitor;
import gray.light.book.service.LocalCacheBookService;
import gray.light.owner.entity.ProjectDetails;
import gray.light.owner.entity.ProjectStatus;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * 遍历本地文档仓库，获取目录、章节等信息，并将其储存在{@link DocumentRepositoryVisitor}里返回。
 * 如果其中有遍历期间发生异常，则将文档状态设置为{@link ProjectStatus#INVALID}无效状态
 *
 * @author XyParaCrim
 */
@CommonsLog
@RequiredArgsConstructor
public class VisitDocumentRepositoryStep extends AbstractExecuteStep<ProjectDetails> {

    /**
     * 遍历本地文档仓库，获取目录、章节执行结果
     *
     * @author XyParaCrim
     */
    public static class Result {

        @Getter
        private final List<DocumentRepositoryVisitor> visitors = new LinkedList<>();

    }

    @NonNull
    private final LocalCacheBookService localCacheBookService;

    /**
     * 根据一组文档实体，遍历仓库文件
     *
     * @param docs 文档
     * @return 执行结果
     */
    public Result execute(@NonNull List<ProjectDetails> docs) {

        CompletableFuture<?>[] asyncTasks = docs.
                stream().
                map(
                        document -> CompletableFuture.
                                supplyAsync(new AsyncVisitTask(document)).
                                handle((v, e) -> completeProcessUnknownException(document, v, e))
                ).toArray(CompletableFuture[]::new);

        return result(asyncTasks, docs);
    }

    /**
     * 当一个异步任务在执行中发生了未检测的错误，则将此错误创建一个错误
     * 的{@link DocumentRepositoryVisitor}，以保证每个异步任务返回
     * 非空的文件浏览器
     *
     * @param document 文档
     * @param visitor  文档仓库浏览器
     * @param e        未检测异常
     * @return 文档仓库浏览器
     */
    private DocumentRepositoryVisitor completeProcessUnknownException(ProjectDetails document, DocumentRepositoryVisitor visitor, Throwable e) {
        return e != null ?
                DocumentRepositoryVisitor.failedVisitor(document, e) :
                visitor;
    }

    /**
     * 通过所有异步任务构造步骤执行结果。这里会阻塞直至所有的异步任务完成，
     * 特别地，每个异步任务会返回其所要处理的文档，文档的{@link ProjectStatus}
     * 表明了该文档的执行是否是真正成功，但是，若请求的文档本身的状态为无效
     * 的，则同样会将其加入到失败结果中
     *
     * @param asyncTasks 异步任务
     * @param docs       文档
     * @return 执行结果
     */
    private Result result(CompletableFuture<?>[] asyncTasks, List<ProjectDetails> docs) {
        Result result = new Result();

        try {
            CompletableFuture.allOf(asyncTasks).get();
        } catch (InterruptedException e) {
            log.error("The thread waiting for the cloning task to complete receives the interrupt signal", e);

            docs.forEach(document -> document.setStatus(ProjectStatus.INVALID));

            return result;
        } catch (ExecutionException e) {
            log.error("Failed to walk document repository in local", e);

            docs.forEach(document -> document.setStatus(ProjectStatus.INVALID));

            return result;
        }

        for (CompletableFuture<?> task : asyncTasks) {
            DocumentRepositoryVisitor visitor = null;

            try {
                visitor = (DocumentRepositoryVisitor) task.get();
            } catch (InterruptedException | ExecutionException e) {
                assert false;
            }

            if (visitor.getFailed() != null) {
                failed(visitor.getDocument(), visitor.getFailed());
            } else {
                success(visitor.getDocument());
                result.visitors.add(visitor);
            }

        }

        return result;
    }

    @RequiredArgsConstructor
    private class AsyncVisitTask implements Supplier<DocumentRepositoryVisitor> {

        final ProjectDetails document;

        @Override
        public DocumentRepositoryVisitor get() {
            try {
                return localCacheBookService.visitRepository(document);
            } catch (InterruptedException e) {
                // 执行线程若中断，则立即退出
                return DocumentRepositoryVisitor.failedVisitor(document, e);
            }
        }
    }

}
