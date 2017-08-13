package com.xmisao.mojix;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;
import org.atilika.kuromoji.Tokenizer.Builder;
import org.atilika.kuromoji.Tokenizer.Mode;

import com.xmisao.mojix.tokenizer.SimpleRequest;
import com.xmisao.mojix.tokenizer.SimpleResponse;
import com.xmisao.mojix.tokenizer.TokenizerGrpc.TokenizerImplBase;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class MojixServer {
	private static final Logger logger = Logger.getLogger(MojixServer.class.getName());

	private Server server;

	private void start() throws IOException {
		int port = 9661;
		server = ServerBuilder.forPort(port).addService(new TokenizerImpl()).build().start();
		logger.info("Mojix started, listening on " + port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				MojixServer.this.stop();
				System.err.println("*** Mojix shut down");
			}
		});
	}

	private void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

	private void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		final MojixServer server = new MojixServer();
		server.start();
		server.blockUntilShutdown();
	}

	static class TokenizerImpl extends TokenizerImplBase {
		private Tokenizer normalTokenizer;
		private Tokenizer searchTokenizer;
		private Tokenizer extendedTokenizer;

		@Override
		public void tokenizeSimply(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
			Tokenizer tokenizer = getTokenizer(request.getMode());

			String text = request.getText();

			SimpleResponse.Builder responseBuilder = SimpleResponse.newBuilder();

			List<String> tokens = new ArrayList<String>();
			for (Token token : tokenizer.tokenize(text)) {
				tokens.add(token.getSurfaceForm());
			}
			responseBuilder.addAllTokens(tokens);

			SimpleResponse response = responseBuilder.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}

		private Tokenizer getTokenizer(SimpleRequest.Mode mode) {
			switch (mode) {
			case NORMAL:
				if (normalTokenizer == null) {
					normalTokenizer = buildTokenizer(Mode.NORMAL);
				}
				return normalTokenizer;
			case SEARCH:
				if (searchTokenizer == null) {
					searchTokenizer = buildTokenizer(Mode.SEARCH);
				}
				return searchTokenizer;
			case EXTENDED:
				if (extendedTokenizer == null) {
					extendedTokenizer = buildTokenizer(Mode.EXTENDED);
				}
				return extendedTokenizer;
			default:
				throw new IllegalArgumentException();
			}
		}

		private Tokenizer buildTokenizer(Mode mode) {
			Builder builder = Tokenizer.builder();
			builder.mode(mode);
			return builder.build();
		}
	}
}
