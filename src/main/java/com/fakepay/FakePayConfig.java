package com.fakepay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FakePayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fakepay.json");

    private FakePayConfig() {}

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            try (Reader reader = Files.newBufferedReader(FILE)) {
                Data data = GSON.fromJson(reader, Data.class);
                if (data != null) {
                    FakePayClient.fakeMode = data.fakeMode;
                    FakePayClient.fakeBalance = Math.max(0.0, data.fakeBalance);
                }
            }
        } catch (Exception ignored) {
            // Use safe defaults if the config cannot be read.
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data data = new Data();
            data.fakeMode = FakePayClient.fakeMode;
            data.fakeBalance = FakePayClient.fakeBalance;
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception ignored) {
            // Config persistence is best-effort.
        }
    }

    private static final class Data {
        boolean fakeMode = false;
        double fakeBalance = 0.0;
    }
}
