package com.robsutar.rnu;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.net.URI;

public class TextureProviderBytes {
    private final IResourcePackNoUploadInternal rnu;
    private final InetSocketAddress address;
    private final URI uri;
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public TextureProviderBytes(IResourcePackNoUploadInternal rnu, String publicLinkRoot, int port) {
        this.rnu = rnu;
        address = new InetSocketAddress(port);
        uri = URI.create(publicLinkRoot);
    }

    public InetSocketAddress address() {
        return address;
    }

    public URI uri() {
        return uri;
    }

    public void run(Runnable beforeLock) throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

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
            try {
                Future<?> channelClose = channel.close();
                Future<?> bossGroupShutdown = bossGroup.shutdownGracefully();
                Future<?> workerGroupShutdown = workerGroup.shutdownGracefully();

                channelClose.sync();
                bossGroupShutdown.sync();
                workerGroupShutdown.sync();
            } catch (Exception ignored) {
            }

            channel = null;
            bossGroup = null;
            workerGroup = null;
        }
    }

    private class LastChildHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            ResourcePackState state = rnu.resourcePackState();
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

    public static TextureProviderBytes deserialize(IResourcePackNoUploadInternal rnu) {
        return new TextureProviderBytes(rnu, rnu.serverConfig().publicLinkRoot(), rnu.serverConfig().port());
    }
}
