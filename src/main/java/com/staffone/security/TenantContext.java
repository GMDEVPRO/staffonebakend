package com.staffone.security;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> CTX = new ThreadLocal<>();
    public static void set(UUID id) { CTX.set(id); }
    public static UUID  get()       { return CTX.get(); }
    public static void  clear()     { CTX.remove(); }
    private TenantContext() {}
}
