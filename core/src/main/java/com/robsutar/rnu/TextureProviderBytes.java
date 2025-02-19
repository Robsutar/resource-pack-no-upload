package com.robsutar.rnu;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.InetSocketAddress;
import java.net.URI;

public abstract class TextureProviderBytes {
    private final InetSocketAddress address;
    private final URI uri;
    private Channel channel;

    public TextureProviderBytes(String addressStr, int port) {
        address = new InetSocketAddress(addressStr, port);
        uri = URI.create("http://" + addressStr + ":" + port);
    }

    public InetSocketAddress address() {
        return address;
    }

    public URI uri() {
        return uri;
    }

    public abstract ResourcePackState state();

    public void run(Runnable beforeLock) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            channel = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(65536));
                            p.addLast(new LastChildHandler());
                        }
                    })
                    .bind(address)
                    .sync()
                    .channel();

            beforeLock.run();

            try {
                channel.closeFuture().sync();
            } catch (InterruptedException ignored) {
            }
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
    }

    private class LastChildHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            ResourcePackState state = state();
            if (state instanceof ResourcePackState.Loaded) {
                ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) state;
                if (loaded.resourcePackInfo().uri().endsWith(request.uri())) {

                    byte[] bytes = loaded.bytes();

                    FullHttpResponse response = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.OK,
                            Unpooled.wrappedBuffer(bytes)
                    );

                    response.headers()
                            .set(HttpHeaderNames.CONTENT_TYPE, "application/zip")
                            .set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"pack.zip\"")
                            .set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

                    ctx.writeAndFlush(response)
                            .addListener(ChannelFutureListener.CLOSE);
                    return;
                }
            }
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            ctx.writeAndFlush(response)
                    .addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Close without logging
            ctx.close();
        }
    }
}
