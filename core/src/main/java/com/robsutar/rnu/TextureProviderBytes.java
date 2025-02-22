package com.robsutar.rnu;

import com.robsutar.rnu.util.OC;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;

public class TextureProviderBytes {
    private final StateProvider stateProvider;
    private final InetSocketAddress address;
    private final URI uri;
    private Channel channel;

    public TextureProviderBytes(StateProvider stateProvider, String publicLinkRoot, int port) {
        this.stateProvider = stateProvider;
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
            ResourcePackState state = stateProvider.resourcePackState();
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

    public static TextureProviderBytes deserialize(StateProvider stateProvider, @Nullable String serverIp, Map<String, Object> raw) {
        if (raw.get("port") == null)
            throw new IllegalArgumentException(
                    "Port undefined in configuration!\n" +
                            "Define it in ResourcePackNoUpload server.yml config\n" +
                            "Make sure to open this port to the players.\n"
            );
        int port = OC.intValue(raw.get("port"));

        String publicLinkRoot;
        if (raw.get("publicLinkRoot") != null) publicLinkRoot = OC.str(raw.get("publicLinkRoot"));
        else {
            if (serverIp != null && !serverIp.isEmpty()) publicLinkRoot = serverIp;
            else try {
                publicLinkRoot = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Failed to get server address from program ipv4.");
            }
            publicLinkRoot = "http://" + publicLinkRoot + ":" + port;
        }

        return new TextureProviderBytes(stateProvider, publicLinkRoot, port);
    }

    public interface StateProvider {
        ResourcePackState resourcePackState();
    }
}
