package com.xjeffrose.xio.guice;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Providers;
import com.xjeffrose.xio.core.HttpServerDef;
import com.xjeffrose.xio.core.NettyServerConfig;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

public abstract class XioModule extends AbstractModule {
  private boolean configBound = false;

  @Override
  protected void configure() {
    configureXio();
  }

  @Provides
  @Singleton
  public ChannelGroup getChannelGroup() {
    return new DefaultChannelGroup();
  }

  public XioModule useDefaultNettyServerConfig() {
    withNettyServerConfig(new Provider<NettyServerConfig>() {
      @Override
      public NettyServerConfig get() {
        return NettyServerConfig.newBuilder().build();
      }
    });
    return this;
  }

  public XioModule withNettyServerConfig(Class<? extends Provider<NettyServerConfig>> providerClass) {
    if (!configBound) {
      binder().bind(NettyServerConfig.class).toProvider(providerClass);
      configBound = true;
      return this;
    }
    throw iae();
  }

  public XioModule withNettyServerConfig(Provider<NettyServerConfig> provider) {
    if (!configBound) {
      // workaround for guice issue # 487
      com.google.inject.Provider<NettyServerConfig> guiceProvider = Providers.guicify(provider);
      binder().bind(NettyServerConfig.class).toProvider(guiceProvider);
      configBound = true;
      return this;
    }
    throw iae();
  }

  /**
   * User of Xio via guice should override this method and use the little DSL defined here.
   */
  protected abstract void configureXio();

  protected XioBuilder bind() {
    return new XioBuilder();
  }

  private IllegalStateException iae() {
    return new IllegalStateException("Config already bound! Call useDefaultNettyServerConfig() or withNettyServerConfig() only once");
  }

  protected class XioBuilder {
    public XioBuilder() {
    }

    public void toInstance(HttpServerDef def) {
      Multibinder.newSetBinder(binder(), HttpServerDef.class)
          .addBinding().toInstance(def);
    }

    public void toProvider(Class<? extends Provider<HttpServerDef>> provider) {
      Multibinder.newSetBinder(binder(), HttpServerDef.class)
          .addBinding().toProvider(provider).asEagerSingleton();
    }

    public void toProvider(Provider<? extends HttpServerDef> provider) {
      // workaround for guice issue # 487
      com.google.inject.Provider<? extends HttpServerDef> guiceProvider = Providers.guicify(provider);
      Multibinder.newSetBinder(binder(), HttpServerDef.class)
          .addBinding().toProvider(guiceProvider).asEagerSingleton();
    }

    public void toProvider(TypeLiteral<? extends javax.inject.Provider<? extends HttpServerDef>> typeLiteral) {
      Multibinder.newSetBinder(binder(), HttpServerDef.class)
          .addBinding().toProvider(typeLiteral).asEagerSingleton();
    }

    public void toProvider(com.google.inject.Key<? extends javax.inject.Provider<? extends HttpServerDef>> key) {
      Multibinder.newSetBinder(binder(), HttpServerDef.class)
          .addBinding().toProvider(key).asEagerSingleton();
    }

    public void to(Class<? extends HttpServerDef> clazz) {
      Multibinder.newSetBinder(binder(), HttpServerDef.class)
          .addBinding().to(clazz).asEagerSingleton();
    }

    public void to(TypeLiteral<? extends HttpServerDef> typeLiteral) {
      Multibinder.newSetBinder(binder(), HttpServerDef.class)
          .addBinding().to(typeLiteral).asEagerSingleton();
    }

    public void to(com.google.inject.Key<? extends HttpServerDef> key) {
      Multibinder.newSetBinder(binder(), HttpServerDef.class)
          .addBinding().to(key).asEagerSingleton();
    }
  }
}