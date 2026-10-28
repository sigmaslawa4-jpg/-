package com.toiletmod.client;

import java.lang.reflect.Method;

/**
 * Доступ к библиотеке MCEF через рефлексию: мод собирается без MCEF,
 * а в игре использует её, если она установлена.
 */
public final class McefBridge {
	private static final String MCEF_CLASS = "com.cinemamod.mcef.MCEF";
	public static final Object NOT_FOUND = new Object();

	private static Class<?> mcef;
	private static boolean logged = false;

	private McefBridge() {}

	public static boolean available() {
		if (mcef != null) return true;
		try {
			mcef = Class.forName(MCEF_CLASS);
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	public static boolean initialized() {
		if (!available()) return false;
		Object r = call(null, mcef, "isInitialized");
		return r instanceof Boolean b && b;
	}

	public static Object createBrowser(String url, boolean transparent) {
		if (!available()) return null;
		Object r = call(null, mcef, "createBrowser", url, transparent);
		return r == NOT_FOUND ? null : r;
	}

	public static int textureId(Object browser) {
		Object renderer = call(browser, "getRenderer");
		if (renderer == null || renderer == NOT_FOUND) return 0;
		Object id = call(renderer, "getTextureID");
		return id instanceof Number n ? n.intValue() : 0;
	}

	/** Вызов метода по имени и числу аргументов (target == null -> статический). */
	public static Object call(Object target, String name, Object... args) {
		return call(target, target.getClass(), name, args);
	}

	public static Object call(Object target, Class<?> cls, String name, Object... args) {
		for (Method m : cls.getMethods()) {
			if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
			try {
				Class<?>[] types = m.getParameterTypes();
				Object[] conv = new Object[args.length];
				for (int i = 0; i < args.length; i++) conv[i] = convert(types[i], args[i]);
				try { m.setAccessible(true); } catch (Throwable ignored) {}
				return m.invoke(target, conv);
			} catch (Throwable t) {
				log("MCEF call failed: " + name + " -> " + t);
			}
		}
		return NOT_FOUND;
	}

	private static Object convert(Class<?> t, Object v) {
		if (v instanceof Number n) {
			if (t == int.class || t == Integer.class) return n.intValue();
			if (t == long.class || t == Long.class) return n.longValue();
			if (t == double.class || t == Double.class) return n.doubleValue();
			if (t == float.class || t == Float.class) return n.floatValue();
		}
		return v;
	}

	private static void log(String msg) {
		if (!logged) {
			logged = true;
			System.err.println("[toiletmod] " + msg + " (повторные ошибки не показываются)");
		}
	}
}
