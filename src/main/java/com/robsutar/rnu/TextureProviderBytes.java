package com.robsutar.rnu;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.InetSocketAddress;

public abstract class TextureProviderBytes {
    private final InetSocketAddress address;

    public TextureProviderBytes(InetSocketAddress address) {
        this.address = address;
    }

    public abstract ResourcePackState state();

    public void run(Runnable beforeLock) throws Exception {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
                                    ResourcePackState state = state();
                                    if (state instanceof ResourcePackState.Loaded) {
                                        ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) state;
                                        byte[] bytes = loaded.bytes();
                                        ByteBuf content = Unpooled.wrappedBuffer(bytes);
                                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
                                        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/zip");
                                        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
                                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                                    } else {
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
                                                .addListener(ChannelFutureListener.CLOSE);
                                    }
                                }
                            });
                        }
                    });

            Channel ch = b.bind(address).sync().channel();

            beforeLock.run();

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
