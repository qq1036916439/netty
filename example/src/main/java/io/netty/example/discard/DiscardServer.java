/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.discard;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * 一个丢弃所有接收到数据的服务
 * Discards any incoming data.
 */
public final class DiscardServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    //
    static final int PORT = Integer.parseInt(System.getProperty("port", "8009"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }
        /**
         * nioEventLoopGroup是处理I/O操作的多线程事件循环。netty为不同类型的传输提供了各种eventloopgroup实现。在本例中，我们实现了一个在服务端的应用程序，因此将使用两个nioEventLoopGroup。第一个，通常称为“boss
         * ”，接受一个新连接。第二个通常称为“worker”，在boss接受连接并将接受的连接注册到worker后，它将处理接受的连接的通信量。使用了多少线程以及如何将它们映射到创建的通道取决于eventloopgroup实现，甚至可以通过构造函数进行配置。
         */
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);  //

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            /**
             * ServerBootstrap是一个帮助我们创建服务。你可以直接使用这个类创建一个服务。请注意这是一个乏味的过程。
             * handler在初始化时就会执行，而childHandler会在客户端成功connect后才执行。
             *
             */
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup) //
                    .channel(NioServerSocketChannel.class) // NioServerSocketChannel 创建新的channel
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() { //ChannelInitializer是一个特殊的处理程序.用来帮助用户创建
                // 新的Channel.你可能在刺穿添加更多的处理程序。并将此类匿名类提取到顶级类中。
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    p.addLast(new DiscardServerHandler());
                }
            }).childOption(ChannelOption.SO_KEEPALIVE, true);
            // 你也可以设置一个参数.指定具体使用那个Channel的实现类。我们想写一个 TCP/IP的服务器。所以我们设置选择option
            // TCP的长连接。请参考api文档指定哪个ChannelConfig和ChannelOption


            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(PORT).sync();
            /**
             * 我们现在已经准备就绪.我们开始绑定端口开始我们服务。我们绑定8080端口。bind可以多次绑定。已用于绑定不同的端口。
             */
            //
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
