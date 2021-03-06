package com.kingston.mmorpg.framework.net.socket;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.kingston.mmorpg.framework.net.socket.annotation.MessageMeta;
import com.kingston.mmorpg.framework.net.socket.message.Message;
import com.kingston.mmorpg.framework.net.socket.serializer.Serializer;

import io.netty.buffer.ByteBuf;

@Component
public class MessageFactory implements ApplicationContextAware {

	private Map<String, Class<? extends Message>> message2Class = new HashMap<>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(MessageMeta.class);

		for (Map.Entry<String, Object> entry : beans.entrySet()) {
			Object message = entry.getValue();
			Class clazz = message.getClass();
			MessageMeta annotation = (MessageMeta) clazz.getAnnotation(MessageMeta.class);
			short module = annotation.module();
			short cmd = annotation.cmd();
			String key = getKey(module, cmd);
			if (message2Class.containsKey(key)) {
				throw new IllegalStateException("模块号[" + key + "]冲突");
			}
			message2Class.put(key, clazz);
		}
	}

	/**
	 * 返回消息的模板class
	 * 
	 * @param module
	 * @param cmd
	 * @return
	 */
	public Class<? extends Message> getMessageMeta(short module, short cmd) {
		String key = getKey(module, cmd);
		return getMessageMeta(key);
	}

	public Class<? extends Message> getMessageMeta(String key) {
		Class<? extends Message> clazz = message2Class.get(key);
		return clazz;
	}

	public void writeMessage(ByteBuf out, Message message) throws Exception {
		Class<?> clazz = message.getClass();
		MessageMeta annotation = (MessageMeta) clazz.getAnnotation(MessageMeta.class);
		short module = annotation.module();
		short cmd = annotation.cmd();
		out.writeShort(module);
		out.writeShort(cmd);

		Serializer serializer = Serializer.getSerializer(message.getClass());
		serializer.encode(out, message, null);
	}

	private String getKey(short module, short cmd) {
		String key = module + "_" + cmd;
		return key;
	}

}
