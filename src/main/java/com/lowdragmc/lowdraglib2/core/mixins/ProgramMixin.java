package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.shader.LDProgramDefineManager;
import com.mojang.blaze3d.shaders.Program;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(Program.class)
public abstract class ProgramMixin {
    @ModifyExpressionValue(method = "compileShaderInternal", at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;process(Ljava/lang/String;)Ljava/util/List;"))
    private static List<String> ldlib2$appendDefines(List<String> original) {
        if (LDProgramDefineManager.hasProgramDefines() && !original.isEmpty()) {
            while (original.getFirst().isBlank()) {
                original = original.subList(1, original.size());
                if (original.isEmpty()) {
                    return original;
                }
            }
            if (!original.isEmpty()) {
                var firstLine = original.getFirst();
                var matcher = LDLibShaders.REGEX_VERSION.matcher(firstLine);
                var defineLine = LDProgramDefineManager.createProgramDefinesString();
                String newFirstLine;
                if (matcher.find()) {
                    int insertPos = matcher.end(); // invert after the end of the version
                    // find a new line
                    int lineEnd = firstLine.indexOf('\n', insertPos);
                    if (lineEnd != -1) {
                        newFirstLine = firstLine.substring(0, lineEnd + 1)
                                + defineLine + "\n"
                                + firstLine.substring(lineEnd + 1);
                    } else {
                        newFirstLine = firstLine + "\n" + defineLine;
                    }
                } else {
                    // no version defined
                    newFirstLine = defineLine + "\n" + firstLine;
                }
                var result = new ArrayList<String>();
                result.add(newFirstLine);
                result.addAll(original.subList(1, original.size()));
                return result;
            }
        }
        return original;
    }
}
