package com.lowdragmc.lowdraglib2.core.mixins.shader;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.shader.LDProgramDefineManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.InputStream;
import java.util.*;

@Mixin(Program.class)
public abstract class ProgramMixin {
//    @Shadow
//    private int id;
//    @Unique
//    private Set<String> ldlib2$defines = Collections.emptySet();
//
//    @Override
//    public Set<String> ldlib2_neoforge$getDefines() {
//        return ldlib2$defines;
//    }
//
//    @Override
//    public int ldlib2_neoforge$getProgramId() {
//        return this.id;
//    }
//
//    @Inject(method = "<init>", at = @At("RETURN"))
//    private void ldlib2$initDefines(Program.Type type, int id, String name, CallbackInfo ci) {
//        if (LDProgramDefineManager.hasProgramDefines()) {
//            ldlib2$defines.addAll(Collections.unmodifiableSet(LDProgramDefineManager.getProgramDefines()));
//        }
//    }

    @WrapMethod(method = "compileShader")
    private static Program ldlib2$compileShader(Program.Type type, String name,
                                                InputStream shaderData, String sourceName,
                                                GlslPreprocessor preprocessor,
                                                Operation<Program> original) {
        if (LDProgramDefineManager.hasProgramDefines()) {
            name = LDProgramDefineManager.createProgramNameWithDefines(name);
        }
        return original.call(type, name, shaderData, sourceName, preprocessor);
    }

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
