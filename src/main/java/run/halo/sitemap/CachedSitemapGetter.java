package run.halo.sitemap;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;

@Component
@AllArgsConstructor
public class CachedSitemapGetter {

    private final AsyncCache<String, String> cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(30))
        .maximumSize(8)
        .buildAsync();

    /**
     * 采集**所有**实现了 {@link SitemapEntryLister} 的扩展（本插件自己的
     * {@link DefaultSitemapEntryLister} 也在其中），这样其它插件就能往站点地图里加条目。
     *
     * <p>这里必须走扩展点而不是直接注入 {@link DefaultSitemapEntryLister}：Halo 里每个插件
     * 各有自己的 {@code PluginApplicationContext}，直接注入具体类时，别的插件注册的实现
     * 永远看不到 —— 接口早就在，但扩展点没打开。
     */
    private final ExtensionGetter extensionGetter;

    public Mono<String> get(SitemapGeneratorOptions options) {
        String key = options.getSiteUrl().toString();
        return Mono.fromFuture(() ->
            cache.get(key, (k, executor) ->
                extensionGetter.getExtensions(SitemapEntryLister.class)
                    .flatMap(lister -> lister.list(options))
                    .collectList()
                    .map(entries -> new SitemapBuilder().buildSitemapXml(entries))
                    .defaultIfEmpty("")
                    .toFuture()
            )
        );
    }
}
