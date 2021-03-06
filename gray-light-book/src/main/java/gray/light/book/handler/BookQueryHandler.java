package gray.light.book.handler;

import gray.light.book.business.CatalogsTreeWalker;
import gray.light.book.business.ContainsCatalogCatalogBo;
import gray.light.book.entity.BookCatalog;
import gray.light.book.entity.BookChapter;
import gray.light.book.service.ReadableBookService;
import gray.light.support.web.RequestParamTables;
import gray.light.support.web.RequestParamVariables;
import gray.light.support.web.RequestSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import perishing.constraint.web.flux.ResponseBuffet;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;

/**
 * 处理查询请求
 *
 * @author XyParaCrim
 */
@Slf4j
@RequiredArgsConstructor
public class BookQueryHandler {

    private final ReadableBookService readableBookService;

    /**
     * 查询文档仓库的结构树
     *
     * @param request 服务请求
     * @return Response of Publisher
     */
    public Mono<ServerResponse> queryBookTree(ServerRequest request) {
        return RequestSupport.extractId(request, projectId -> {
            Tuple2<List<BookCatalog>, List<BookChapter>> queryResult = readableBookService.catalogAndChapter(projectId);
            ContainsCatalogCatalogBo rootBo = CatalogsTreeWalker.walk(queryResult.getT1(), queryResult.getT2());

            return ResponseBuffet.allRight(rootBo.getCatalogs());
        });
    }

    /**
     * 查询文档仓库的结构树
     *
     * @param variables 参数表
     * @return Response of Publisher
     */
    public Mono<ServerResponse> queryBookStructureTree(RequestParamVariables variables) {
        Long projectId = RequestParamTables.ID.get(variables);

        Tuple2<List<BookCatalog>, List<BookChapter>> queryResult = readableBookService.catalogAndChapter(projectId);
        ContainsCatalogCatalogBo rootBo = CatalogsTreeWalker.walk(queryResult.getT1(), queryResult.getT2());

        return ResponseBuffet.allRight(rootBo.getCatalogs());
    }


}
