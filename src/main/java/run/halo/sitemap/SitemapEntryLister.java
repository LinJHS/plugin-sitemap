package run.halo.sitemap;

import org.pf4j.ExtensionPoint;
import reactor.core.publisher.Flux;

/**
 * 站点地图条目来源：实现它就能往 /sitemap.xml 里加条目
 * （本插件自己的 {@link DefaultSitemapEntryLister} 就是其中一员）。
 *
 * <p>必须继承 pf4j 的 {@link ExtensionPoint}：Halo 的 {@code ExtensionGetter}
 * 只按扩展点类型查找实现，缺了这个上界，别的插件注册的实现在运行时发现不了 ——
 * 接口一直存在，但扩展点从未真正打开。
 */
public interface SitemapEntryLister extends ExtensionPoint {

    Flux<SitemapEntry> list(SitemapGeneratorOptions options);
}