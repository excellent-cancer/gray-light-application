package gray.light.book.scheduler.step;

import gray.light.book.DocumentRepositoryVisitor;
import gray.light.book.entity.BookCatalog;
import gray.light.book.entity.BookChapter;
import gray.light.book.service.WritableBookService;
import gray.light.owner.entity.ProjectDetails;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 将最新上传文档和上传文档发生错误的文档状态，新增文档目录、新增文章节
 * 更新至数据库
 *
 * @author XyParaCrim
 */
@RequiredArgsConstructor
public class BatchUpdateDocumentRepositoriesStep {

    @NonNull
    private final WritableBookService writableBookService;

    public void execute(@NonNull List<DocumentRepositoryVisitor> visitors, @NonNull List<ProjectDetails> emptyDocument) {
        List<BookCatalog> catalogs = catalogs(visitors);
        List<BookChapter> chapters = chapters(visitors);

        // 更新文档状态
        writableBookService.batchSyncBookFromPending(emptyDocument, catalogs, chapters);
    }

    private List<BookCatalog> catalogs(List<DocumentRepositoryVisitor> visitors) {
        return visitors.
                stream().
                flatMap(visitor -> visitor.getCatalogs().stream()).
                collect(Collectors.toList());
    }

    private List<BookChapter> chapters(List<DocumentRepositoryVisitor> visitors) {
        return visitors.
                stream().
                flatMap(visitor -> visitor.getChapters().stream().map(Tuple2::getT1)).
                collect(Collectors.toList());
    }
}
