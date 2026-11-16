package run.halo.sitemap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;

@ExtendWith(MockitoExtension.class)
public class CachedSitemapGetterTest {
    // fork（2026-11-16）起 CachedSitemapGetter 走扩展点取条目来源，不再是直接注入具体类，
    // 所以这里改成桩一个 ExtensionGetter，让它返回本插件自己的 DefaultSitemapEntryLister。
    @Mock
    private ExtensionGetter extensionGetter;

    @Mock
    private DefaultSitemapEntryLister lister;

    private CachedSitemapGetter getter;
    private SitemapGeneratorOptions options;

    @BeforeEach
    void setUp() throws MalformedURLException {
        when(extensionGetter.getExtensions(SitemapEntryLister.class))
            .thenReturn(Flux.just(lister));
        when(lister.list(any())).thenReturn(
            Flux.just(SitemapEntry.builder().loc("http://localhost:8090/about").build()));
        getter = new CachedSitemapGetter(extensionGetter);
        options = SitemapGeneratorOptions.builder()
            .siteUrl(new URL("http://localhost:8090"))
            .build();
    }

    @Test
    void get() throws InterruptedException, ExecutionException {
        getter.get(options).block();
        verify(lister).list(any());

        var executorService = Executors.newCachedThreadPool();

        List<? extends Future<?>> futures = IntStream.range(0, 10)
            .mapToObj(i -> executorService.submit(() -> getter.get(options).block()))
            .toList();

        for (Future<?> future : futures) {
            future.get();
        }

        verify(lister, times(1)).list(any());
    }
}
