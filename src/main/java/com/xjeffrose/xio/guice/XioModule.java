package com.xjeffrose.xio.guice;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Providers;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerConfig;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import javax.inject.Provider;
import javax.inject.Singleton;

public abstract class XioModule extends AbstractModule {
  private boolean configBound = false;

  @Override
  protected void configure() {
    configureXio();
  }

  @Provides
  @Singleton
  public ChannelGroup getChannelGroup() {
    return new DefaultChannelGroup(new NioEventLoopGroup().next());
  }

  public XioModule useDefaultNettyServerConfig() {
    withNettyServerConfig(new Provider<XioServerConfig>() {
      @Override
      public XioServerConfig get() {
        return XioServerConfig.newBuilder().build();
      }
    });
    return this;
  }

  public XioModule withNettyServerConfig(Class<? extends Provider<XioServerConfig>> providerClass) {
    if (!configBound) {
      binder().bind(XioServerConfig.class).toProvider(providerClass);
      configBound = true;
      return this;
    }
    throw iae();
  }

  public XioModule withNettyServerConfig(Provider<XioServerConfig> provider) {
    if (!configBound) {
      // workaround for guice issue # 487
      com.google.inject.Provider<XioServerConfig> guiceProvider = Providers.guicify(provider);
      binder().bind(XioServerConfig.class).toProvider(guiceProvider);
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

    public void toInstance(XioServerDef def) {
      Multibinder.newSetBinder(binder(), XioServerDef.class)
          .addBinding().toInstance(def);
    }

    public void toProvider(Class<? extends Provider<XioServerDef>> provider) {
      Multibinder.newSetBinder(binder(), XioServerDef.class)
          .addBinding().toProvider(provider).asEagerSingleton();
    }

    public void toProvider(Provider<? extends XioServerDef> provider) {
      // workaround for guice issue # 487
      com.google.inject.Provider<? extends XioServerDef> guiceProvider = Providers.guicify(provider);
      Multibinder.newSetBinder(binder(), XioServerDef.class)
          .addBinding().toProvider(guiceProvider).asEagerSingleton();
    }

    public void toProvider(TypeLiteral<? extends javax.inject.Provider<? extends XioServerDef>> typeLiteral) {
      Multibinder.newSetBinder(binder(), XioServerDef.class)
          .addBinding().toProvider(typeLiteral).asEagerSingleton();
    }

    public void toProvider(com.google.inject.Key<? extends javax.inject.Provider<? extends XioServerDef>> key) {
      Multibinder.newSetBinder(binder(), XioServerDef.class)
          .addBinding().toProvider(key).asEagerSingleton();
    }

    public void to(Class<? extends XioServerDef> clazz) {
      Multibinder.newSetBinder(binder(), XioServerDef.class)
          .addBinding().to(clazz).asEagerSingleton();
    }

    public void to(TypeLiteral<? extends XioServerDef> typeLiteral) {
      Multibinder.newSetBinder(binder(), XioServerDef.class)
          .addBinding().to(typeLiteral).asEagerSingleton();
    }

    public void to(com.google.inject.Key<? extends XioServerDef> key) {
      Multibinder.newSetBinder(binder(), XioServerDef.class)
          .addBinding().to(key).asEagerSingleton();
    }
  }
}