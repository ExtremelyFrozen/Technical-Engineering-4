// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jade;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 管道升级 Jade 兼容契约：管道状态显示（PipeJadeProvider）。
 * <p>
 * 用户设计：
 * <ul>
 *   <li>对准管道方块（pipe/pipe_white/pipe_black）显示：过滤模式（白/黑名单）、五种升级层级、IO 速率</li>
 *   <li>getUid = kenergyengineering:pipe_status；注册于 TENJadePlugin（CableBased 基类，线缆经守卫过滤）</li>
 *   <li>升级状态经 PipeBlockEntity 既有 getter 读取（getUpgradeLevel/singleTransferAmount/isFiltered/isWhitelist/isBlacklist）</li>
 *   <li>升级层级显示：仅激活升级（level>0）按枚举序（Pull/Push/Speed/Page/Ender）各占一行，未激活升级不显示</li>
 *   <li>lang：复用 pipe.filter.whitelist/blacklist 与 pipe.upgrade.*，新增 pipe.jade.io_rate / pipe.jade.pages</li>
 * </ul>
 * <p>
 * 使用源码模式扫描（项目既有约定）——client/test sourceSet 不交叉编译（test 仅含 main），
 * 且 MC/Jade 运行时无法在纯 JUnit 中 bootstrap；语义断言锚定源码结构，避免运行时引导。
 */
class PipeJadeContractTest {

    private static final String CLIENT_ROOT = "src/client/java/";
    private static final String MAIN_ROOT = "src/main/java/";

    private static final String PROVIDER_SRC = "com/modularmc/ten/integration/jade/PipeJadeProvider.java";
    private static final String PLUGIN_SRC = "com/modularmc/ten/integration/jade/TENJadePlugin.java";
    private static final String PIPE_SRC = "com/modularmc/ten/common/blockentity/PipeBlockEntity.java";
    private static final String TYPE_SRC = "com/modularmc/ten/common/blockentity/PipeUpgradeType.java";
    private static final String LANG_SRC = "com/modularmc/ten/data/lang/TENLangHandler.java";
    private static final String EN_JSON = "src/generated/resources/assets/kenergyengineering/lang/en_us.json";
    private static final String ZH_JSON = "src/generated/resources/assets/kenergyengineering/lang/zh_cn.json";

    static String readSource(String relativePath) throws Exception {
        var f = new File(MAIN_ROOT + relativePath);
        assertTrue(f.exists(), "Source file must exist: " + relativePath);
        return Files.readString(f.toPath());
    }

    static String readClientSource(String relativePath) throws Exception {
        var f = new File(CLIENT_ROOT + relativePath);
        assertTrue(f.exists(), "Client source file must exist: " + relativePath);
        return Files.readString(f.toPath());
    }

    static String readFile(String path) throws Exception {
        var f = new File(path);
        assertTrue(f.exists(), "File must exist: " + path);
        return Files.readString(f.toPath());
    }

    static String methodBody(String src, String methodHeader) throws Exception {
        int methodIdx = src.indexOf(methodHeader);
        assertTrue(methodIdx >= 0, "Method must exist: " + methodHeader);
        int bodyStart = src.indexOf('{', methodIdx);
        int bodyEnd = findMatchingBrace(src, bodyStart);
        return src.substring(bodyStart, bodyEnd);
    }

    static int findMatchingBrace(String s, int openIdx) {
        if (s.charAt(openIdx) != '{') {
            throw new IllegalArgumentException("Character at openIdx must be '{'");
        }
        int depth = 1;
        for (int i = openIdx + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i + 1; // exclusive end
            }
        }
        throw new IllegalArgumentException("No matching brace found from index " + openIdx);
    }

    // ════════════════════════════════════════════════════════════════
    // 1. PipeJadeProvider 存在 + 身份（getUid）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class ProviderIdentity {

        @Test
        void providerExistsInClientSourceSet() throws Exception {
            var src = readClientSource(PROVIDER_SRC);
            assertTrue(src.contains("public class PipeJadeProvider implements IBlockComponentProvider"),
                    "RED: PipeJadeProvider must implement IBlockComponentProvider");
        }

        @Test
        void getUidIsPipeStatus() throws Exception {
            var src = readClientSource(PROVIDER_SRC);
            String body = methodBody(src, "public Identifier getUid");
            assertTrue(body.contains("TEN.id(\"pipe_status\")"),
                    "RED: getUid must return kenergyengineering:pipe_status via TEN.id");
        }

        @Test
        void appendTooltipGuardsPipeBlockEntity() throws Exception {
            var src = readClientSource(PROVIDER_SRC);
            String body = methodBody(src, "public void appendTooltip");
            assertTrue(body.contains("instanceof PipeBlockEntity"),
                    "RED: appendTooltip must guard with instanceof PipeBlockEntity");
            assertTrue(body.contains("return;"),
                    "RED: non-pipe block entities (null / cables) must early-return");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. 状态读取：过滤模式 + 升级层级 + IO 速率
    // ════════════════════════════════════════════════════════════════

    @Nested
    class StatusReadout {

        @Test
        void readsFilterModeFromPipeBlockEntity() throws Exception {
            var src = readClientSource(PROVIDER_SRC);
            String body = methodBody(src, "public void appendTooltip");
            assertTrue(body.contains("pipe.isFiltered()"),
                    "RED: filter line must be gated on isFiltered()");
            assertTrue(body.contains("pipe.isWhitelist()"),
                    "RED: provider must read isWhitelist() for mode selection");
            assertTrue(body.contains("pipe.filter.whitelist") && body.contains("pipe.filter.blacklist"),
                    "RED: provider must reuse pipe.filter.whitelist/blacklist lang keys");
        }

        @Test
        void readsPullSidesFromPipeBlockEntity() throws Exception {
            var src = readClientSource(PROVIDER_SRC);
            String body = methodBody(src, "public void appendTooltip");
            assertTrue(body.contains("pipe.isPullSide"),
                    "RED: provider must read pull sides via isPullSide(direction)");
            assertTrue(body.contains("pipe.jade.pull_side"),
                    "RED: pull side line must use pipe.jade.pull_side lang key");
        }

        @Test
        void readsIoRateFromConstant() throws Exception {
            var src = readClientSource(PROVIDER_SRC);
            String body = methodBody(src, "public void appendTooltip");
            assertTrue(body.contains("PipeBlockEntity.TRANSFER_RATE"),
                    "RED: IO rate must come from PipeBlockEntity.TRANSFER_RATE constant (unified 64/tick)");
            assertTrue(body.contains("pipe.jade.io_rate"),
                    "RED: IO rate line must use pipe.jade.io_rate lang key");
        }

        @Test
        void readsBufferState() throws Exception {
            var src = readClientSource(PROVIDER_SRC);
            String body = methodBody(src, "public void appendTooltip");
            assertTrue(body.contains("pipe.getBuffer()"),
                    "RED: provider must read pipe buffer via getBuffer()");
            assertTrue(body.contains("pipe.jade.buffer"),
                    "RED: buffer line must use pipe.jade.buffer lang key");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. PipeBlockEntity 升级 getter 可被客户端 provider 跨包访问（public）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class PipeBlockEntityAccessors {

        @Test
        void stateGettersArePublic() throws Exception {
            var src = readSource(PIPE_SRC);
            assertTrue(src.contains("public boolean isFiltered()"),
                    "RED: isFiltered() must be public (cross-package provider access)");
            assertTrue(src.contains("public boolean isWhitelist()"),
                    "RED: isWhitelist() must be public");
            assertTrue(src.contains("public boolean isBlacklist()"),
                    "RED: isBlacklist() must be public");
            assertTrue(src.contains("public static final int TRANSFER_RATE = 64"),
                    "RED: TRANSFER_RATE constant must be public (unified 64/tick)");
            assertTrue(src.contains("public boolean isPullSide"),
                    "RED: isPullSide must be public (cross-package provider access)");
        }

        @Test
        void pullSideAccessorsPublic() throws Exception {
            var src = readSource(PIPE_SRC);
            assertTrue(src.contains("public boolean isPullSide(Direction side)"),
                    "RED: isPullSide must be public");
            assertTrue(src.contains("public boolean togglePullSide(Direction side)"),
                    "RED: togglePullSide must be public (spanner interaction)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 4. 注册于 TENJadePlugin
    // ════════════════════════════════════════════════════════════════

    @Nested
    class PluginRegistration {

        @Test
        void pipeProviderRegisteredInRegisterClient() throws Exception {
            var src = readClientSource(PLUGIN_SRC);
            String body = methodBody(src, "public void registerClient");
            assertTrue(body.contains("new PipeJadeProvider()"),
                    "RED: registerClient must instantiate PipeJadeProvider");
            assertTrue(body.contains("CableBased.class"),
                    "RED: pipe provider must be registered against CableBased.class (pipe/pipe_white/pipe_black share it)");
            assertTrue(body.contains("registerBlockComponent"),
                    "RED: must use registerBlockComponent (client-only tooltip provider)");
        }

        @Test
        void channelProviderRegistrationStillPresent() throws Exception {
            var src = readClientSource(PLUGIN_SRC);
            String body = methodBody(src, "public void registerClient");
            assertTrue(body.contains("new ChannelJadeProvider()"),
                    "RED: channel provider registration must not be removed");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 5. 语言键：handler 注册 + generated json 同步
    // ════════════════════════════════════════════════════════════════

    @Nested
    class LangKeys {

        @Test
        void handlerRegistersPipeJadeKeys() throws Exception {
            var src = readSource(LANG_SRC);
            assertTrue(src.contains("addPipeJade()"),
                    "RED: TENLangHandler static block must invoke addPipeJade()");
            assertTrue(src.contains("add(\"pipe.jade.io_rate\""),
                    "RED: missing pipe.jade.io_rate key in handler");
            assertTrue(src.contains("add(\"pipe.jade.buffer\""),
                    "RED: missing pipe.jade.buffer key in handler");
            assertTrue(src.contains("add(\"pipe.jade.pull_side\""),
                    "RED: missing pipe.jade.pull_side key in handler");
        }

        @Test
        void generatedLangFilesContainKeys() throws Exception {
            String en = readFile(EN_JSON);
            String zh = readFile(ZH_JSON);
            assertTrue(en.contains("\"kenergyengineering.pipe.jade.io_rate\""),
                    "RED: en_us.json must contain pipe.jade.io_rate");
            assertTrue(zh.contains("\"kenergyengineering.pipe.jade.io_rate\""),
                    "RED: zh_cn.json must contain pipe.jade.io_rate");
            assertTrue(en.contains("\"kenergyengineering.pipe.jade.buffer\""),
                    "RED: en_us.json must contain pipe.jade.buffer");
            assertTrue(zh.contains("\"kenergyengineering.pipe.jade.buffer\""),
                    "RED: zh_cn.json must contain pipe.jade.buffer");
            assertTrue(zh.contains("\"kenergyengineering.pipe.jade.io_rate\": \"IO 速率: %s 物品/tick\""),
                    "RED: zh_cn.json must contain Chinese IO rate message");
        }
    }
}
