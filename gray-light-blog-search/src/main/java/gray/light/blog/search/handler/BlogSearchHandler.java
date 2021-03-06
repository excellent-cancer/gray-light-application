package gray.light.blog.search.handler;

import floor.cache.StringChannelCache;
import gray.light.blog.business.BlogBo;
import gray.light.blog.search.service.SearchBlogService;
import gray.light.support.web.RequestParam;
import gray.light.support.web.RequestParamTables;
import gray.light.support.web.ScrollPageChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import perishing.constraint.jdbc.Page;
import perishing.constraint.treasure.chest.collection.FinalVariables;
import perishing.constraint.web.flux.ResponseBuffet;
import reactor.core.publisher.Mono;

/**
 * 处理关于搜索的请求，例如缓存scrollId
 *
 * @author XyParaCrim
 */
@Slf4j
@RequiredArgsConstructor
public class BlogSearchHandler {

    public static final RequestParam<String> SEARCH_PARAM = RequestParamTables.paramTable("word");

    public static final RequestParam<String> SCROLL_ID = RequestParamTables.paramTable("scroll");

    private final SearchBlogService searchBlogService;

    private final StringChannelCache searchScrollCache;

    /**
     * 开始新搜索，若这个session持有scrollId，则会将旧的清除
     *
     * @param params 抽象请求参数
     * @param serverRequest 请求
     * @return 回复发布
     */
    public Mono<ServerResponse> search(FinalVariables<String> params, ServerRequest serverRequest) {
        return serverRequest.
                session().
                flatMap(webSession -> {
                    String searchWord = SEARCH_PARAM.get(params);
                    Page page = RequestParamTables.page().get(params);
                    ScrollPageChunk<BlogBo> hits = searchBlogService.search(searchWord, page);

                    searchScrollCache.cache(webSession.getId(), hits.getScrollId(), ids -> ids.forEach(searchBlogService::clearSearch));

                    return ResponseBuffet.allRight(hits);
                });
    }


    /**
     * 持续搜索，搜索成功后，将继续缓存
     *
     * @param params 抽象请求参数
     * @param serverRequest 请求
     * @return 回复发布
     */
    public Mono<ServerResponse> keepSearch(FinalVariables<String> params, ServerRequest serverRequest) {
        return serverRequest.
                session().
                flatMap(webSession -> {
                    String scrollId = SCROLL_ID.get(params);
                    ScrollPageChunk<BlogBo> hits = searchBlogService.continueSearch(scrollId);

                    searchScrollCache.cache(webSession.getId(), hits.getScrollId(), ids -> ids.forEach(searchBlogService::clearSearch));

                    return ResponseBuffet.allRight(hits);
                });
    }


    /**
     * 完成整个搜索，告诉es释放资源
     *
     * @param params 抽象请求参数
     * @param serverRequest 请求
     * @return 回复发布
     */
    public Mono<ServerResponse> finishSearch(FinalVariables<String> params, ServerRequest serverRequest) {
        return serverRequest.
                session().
                flatMap(webSession -> {
                    String scrollId = SCROLL_ID.get(params);

                    searchBlogService.clearSearch(scrollId);
                    searchScrollCache.remove(webSession.getId(), scrollId);

                    return ResponseBuffet.allRight();
                });
    }

}
