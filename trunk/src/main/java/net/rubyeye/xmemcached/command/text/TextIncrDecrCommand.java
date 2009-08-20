package net.rubyeye.xmemcached.command.text;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.command.Command;
import net.rubyeye.xmemcached.command.CommandType;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.MemcachedTCPSession;
import net.rubyeye.xmemcached.monitor.Constants;
import net.rubyeye.xmemcached.utils.ByteUtils;

public class TextIncrDecrCommand extends Command {

	private int increment;

	public TextIncrDecrCommand(String key, byte[] keyBytes,
			CommandType cmdType, CountDownLatch latch, int increment,
			boolean noreply) {
		super(key, keyBytes, cmdType, latch);
		this.increment = increment;
		this.noreply = noreply;
	}

	public final int getIncrement() {
		return this.increment;
	}

	public final void setIncrement(int increment) {
		this.increment = increment;
	}

	public static final byte[] INCR = { 'i', 'n', 'c', 'r' };
	public static final byte[] DECR = { 'd', 'e', 'c', 'r' };

	@Override
	public final boolean decode(MemcachedTCPSession session, ByteBuffer buffer) {
		String line = ByteUtils.nextLine(buffer);
		if (line != null) {
			if (line.equals("NOT_FOUND")) {
				setException(new MemcachedException(
						"The key's value is not found for increase or decrease"));
				countDownLatch();
				return true;
			} else {
				setResult(Integer.parseInt(line));
				countDownLatch();
				return true;
			}
		}
		return false;
	}

	@Override
	public final void encode(BufferAllocator bufferAllocator) {
		byte[] numBytes = ByteUtils.getBytes(String
				.valueOf(this.getIncrement()));
		byte[] cmdBytes = this.commandType == CommandType.INCR ? INCR : DECR;
		int capacity = cmdBytes.length + 2 + this.key.length()
				+ numBytes.length + Constants.CRLF.length;
		if (isNoreply()) {
			capacity += 1 + Constants.NO_REPLY.length();
		}
		this.ioBuffer = bufferAllocator.allocate(capacity);
		if (isNoreply()) {
			ByteUtils.setArguments(this.ioBuffer, cmdBytes, this.keyBytes,
					numBytes, Constants.NO_REPLY);
		} else {
			ByteUtils.setArguments(this.ioBuffer, cmdBytes, this.keyBytes,
					numBytes);
		}
		this.ioBuffer.flip();

	}

}
