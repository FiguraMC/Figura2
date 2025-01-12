package org.figuramc.figura.model.optimized;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.CompiledShader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.Util;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import org.figuramc.figura.FiguraMod;

import java.util.List;
import java.util.function.Function;

/**
 * Contains various items related to Figura's optimized rendering mode.
 *
 * Extend RenderType for that sweet, sweet protected member access.
 *
 * TODO: Fix this horrible and evil code for creating the ShaderInstance
 */
public class OptimizedRendering extends RenderType {

    private OptimizedRendering(String string, VertexFormat vertexFormat, VertexFormat.Mode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2) {
        super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
    }

    public static void init() {
        // Just needs to load the class
    }

    /**
     * The Position, Normal, and UV elements are as usual.
     * We use the other elements to store additional data for skinning.
     * Each vertex holds reference to 4 model part IDs.
     * A model part ID is a short, and we have 4 of them by combining UV1 and UV2.
     * Also, each vertex has 4 weights from 0 to 1 indicating how much each of the 4 parts should affect the vertex.
     * These 4 weights are stored in the 4 RGBA channels of the COLOR attribute.
     */
    public static final VertexFormat OPTIMIZED_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("UV", VertexFormatElement.UV)
            .add("PartID_1", VertexFormatElement.UV1)
            .add("PartID_2", VertexFormatElement.UV2)
            .add("Weights", VertexFormatElement.COLOR)
            .build();

    public static final ResourceLocation PLACEHOLDER = FiguraMod.id("unspecified_shader");

    public static final CompiledShaderProgram OPTIMIZED_SHADER;

    static {
        String vertexSrc = """
                #version 460
                
                in vec3 Position;
                in vec3 Normal;
                in vec2 UV;
                in ivec2 PartID_1;
                in ivec2 PartID_2;
                in vec4 Weights;
                
                uniform mat4 ModelViewMat; // Sends world -> view
                uniform mat4 ProjMat; // Sends view -> clip
                uniform mat4 FiguraRootMatrix; // Custom matrix uniform, sends model -> world
                
                uniform vec3 Light0_Direction;
                uniform vec3 Light1_Direction;
                
                struct PartData {
                    mat4 transform; // Position transform for the part
                    mat3 normalMat; // Normal matrix for the part
                };
                layout(binding = 0, std430) readonly buffer PartDataBuffer {
                    PartData parts[];
                };
                
                out vec3 worldSpaceNormal;
                out vec4 vertColor;
                out vec2 vertUV;
                
                void main() {
                    // Define vectors, then accumulate them from the (up to) 4 transforms
                    vec4 pos = vec4(0.0);
                    vec3 normal = vec3(0.0);
                    if (PartID_1.x != -1) {
                        PartData part = parts[PartID_1.x];
                        pos += Weights.x * part.transform * vec4(Position, 1.0);
                        normal += Weights.x * part.normalMat * Normal;
                    }
                    if (PartID_1.y != -1) {
                        PartData part = parts[PartID_1.y];
                        pos += Weights.y * part.transform * vec4(Position, 1.0);
                        normal += Weights.y * part.normalMat * Normal;
                    }
                    if (PartID_2.x != -1) {
                        PartData part = parts[PartID_2.x];
                        pos += Weights.z * part.transform * vec4(Position, 1.0);
                        normal += Weights.z * part.normalMat * Normal;
                    }
                    if (PartID_2.y != -1) {
                        PartData part = parts[PartID_2.y];
                        pos += Weights.w * part.transform * vec4(Position, 1.0);
                        normal += Weights.w * part.normalMat * Normal;
                    }
                
                    // Outputs
                    gl_Position = ProjMat * ModelViewMat * FiguraRootMatrix * pos;
                    worldSpaceNormal = normalize((FiguraRootMatrix * vec4(normal, 0.0)).xyz);
                    vertColor = vec4(1.0);
                    vertUV = UV;
                }
                """;
        String fragmentSrc = """
                #version 460
                
                in vec3 worldSpaceNormal;
                in vec4 vertColor;
                in vec2 vertUV;
                
                uniform sampler2D Sampler0;
                
                uniform vec3 Light0_Direction;
                uniform vec3 Light1_Direction;
                
                out vec4 fragColor;
                
                #define MINECRAFT_LIGHT_POWER   (0.6)
                #define MINECRAFT_AMBIENT_LIGHT (0.4)
                vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {
                    float light0 = max(0.0, dot(lightDir0, normal));
                    float light1 = max(0.0, dot(lightDir1, normal));
                    float lightAccum = min(1.0, (light0 + light1) * MINECRAFT_LIGHT_POWER + MINECRAFT_AMBIENT_LIGHT);
                    return vec4(color.rgb * lightAccum, color.a);
                }
                
                void main() {
                    vec4 color = texture(Sampler0, vertUV) * vertColor;
                    if (color.a < 0.1) {
                        discard;
                    }
                    vec4 lit = minecraft_mix_light(Light0_Direction, Light1_Direction, normalize(worldSpaceNormal), color);
                    fragColor = lit;
                }
                """;
        try {
            ShaderProgram program = new ShaderProgram(PLACEHOLDER, OPTIMIZED_FORMAT, ShaderDefines.EMPTY);
            CompiledShader vert = CompiledShader.compile(PLACEHOLDER, CompiledShader.Type.VERTEX, GlslPreprocessor.injectDefines(vertexSrc, program.defines()));
            CompiledShader frag = CompiledShader.compile(PLACEHOLDER, CompiledShader.Type.FRAGMENT, GlslPreprocessor.injectDefines(fragmentSrc, program.defines()));
            OPTIMIZED_SHADER = CompiledShaderProgram.link(vert, frag, OPTIMIZED_FORMAT);
            OPTIMIZED_SHADER.setupUniforms(List.of(
                    new ShaderProgramConfig.Uniform("ModelViewMat", "matrix4x4", 16, List.of(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f)),
                    new ShaderProgramConfig.Uniform("ProjMat", "matrix4x4", 16, List.of(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f)),
                    new ShaderProgramConfig.Uniform("Light0_Direction", "float", 3, List.of(0f, 0f, 0f)),
                    new ShaderProgramConfig.Uniform("Light1_Direction", "float", 3, List.of(0f, 0f, 0f)),
                    new ShaderProgramConfig.Uniform("FiguraRootMatrix", "matrix4x4", 16, List.of(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f))
            ), List.of(
                    new ShaderProgramConfig.Sampler("Sampler0")
            ));
        } catch (ShaderManager.CompilationException compEx) {
            throw new IllegalStateException("Failed to compile builtin Figura shader!", compEx);
        }
    }

    public static final Function<ResourceLocation, RenderType> OPTIMIZED_RENDER_TYPE = Util.memoize(texture -> {
        CompositeState compositeState = CompositeState.builder()
                .setLayeringState(SetupFiguraShader.INSTANCE)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, TriState.DEFAULT, false))
                .createCompositeState(true);
        return RenderType.create("figura_optimized", OPTIMIZED_FORMAT, VertexFormat.Mode.QUADS, 4096, compositeState);
    });


    // Cursed that it uses layering shard instead of shader shard, but I don't care right now
    private static class SetupFiguraShader extends LayeringStateShard {
        private SetupFiguraShader() {
            super("figura_shader", () -> RenderSystem.setShader(OPTIMIZED_SHADER), () -> {});
        }
        private static final SetupFiguraShader INSTANCE = new SetupFiguraShader();
    }

}
