package com.hhu.server;

import com.hhu.codec.DecodeHandler;
import com.hhu.codec.EncodeHandler;
import com.hhu.server.console.ConsoleManager;
import com.hhu.server.console.impl.SendFileConsole;
import com.hhu.server.handler.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Scanner;

public class Server {

	private static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));

	public static void main(String[] args) throws InterruptedException {
		ServerBootstrap bootstrap = new ServerBootstrap();

		EventLoopGroup boss = new NioEventLoopGroup();
		EventLoopGroup worker = new NioEventLoopGroup();

		bootstrap.group(boss, worker)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.option(ChannelOption.TCP_NODELAY, true)
				.childHandler(new ChannelInitializer<NioSocketChannel>() {
					@Override
					protected void initChannel(NioSocketChannel channel) throws Exception {
						ChannelPipeline pipeline = channel.pipeline();
						pipeline.addLast(new FileReceiveServerHandler());
						pipeline.addLast(new FileSendServerHandler());
						pipeline.addLast(new DecodeHandler());
						pipeline.addLast(new EncodeHandler());
						pipeline.addLast(new JoinClusterRequestHandler());
						pipeline.addLast(new FilePacketServerHandler());
						// pipeline.addLast("handler", new MyServerHandler());
					}
				});

		ChannelFuture future = bootstrap.bind(PORT).sync();
		if (future.isSuccess()) {
			System.out.println("端口绑定成功");
			Channel channel = future.channel();
			console(channel);
		} else {
			System.out.println("端口绑定失败");
		}

		future.channel().closeFuture().sync();
	}

	private static void console(Channel channel) {
		ConsoleManager consoleManager = new ConsoleManager();
		Scanner scanner = new Scanner(System.in);
		new Thread(() -> {
			while (!Thread.interrupted()) {
				SendFileConsole sendFileConsole = new SendFileConsole();
				sendFileConsole.exec(channel, scanner);
			}
		}).start();
	}



}
