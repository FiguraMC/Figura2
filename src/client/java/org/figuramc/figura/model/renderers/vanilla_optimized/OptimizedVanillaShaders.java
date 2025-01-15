package org.figuramc.figura.model.renderers.vanilla_optimized;

import com.mojang.blaze3d.shaders.CompiledShader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.Util;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Code related to the "VanillaOptimizedRenderer" renderer, which sets up the shaders and other
 * rendering objects for it to use.
 */
public abstract class OptimizedVanillaShaders extends RenderType {

    public static final VertexFormat FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("UV", VertexFormatElement.UV)
            .add("PartID_1", VertexFormatElement.UV1)
            .add("PartID_2", VertexFormatElement.UV2)
            .add("Weights", VertexFormatElement.COLOR)
            .build();

    // Extend for easy protected member access
    private OptimizedVanillaShaders(String string, VertexFormat vertexFormat, VertexFormat.Mode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2) {
        super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
    }

    private static CompiledShaderProgram createOptimizedShader(String name, String vert, String frag) {
        ResourceLocation placeholder = FiguraMod.id("optimized_shaders/" + name);
        try {
            CompiledShader vertC = CompiledShader.compile(placeholder.withPath(s -> s + ".vsh"), CompiledShader.Type.VERTEX, vert);
            CompiledShader fragC = CompiledShader.compile(placeholder.withPath(s -> s + ".fsh"), CompiledShader.Type.FRAGMENT, frag);
            CompiledShaderProgram program = CompiledShaderProgram.link(vertC, fragC, FORMAT);
            program.setupUniforms(List.of(
                    new ShaderProgramConfig.Uniform("ModelViewMat", "matrix4x4", 16, List.of(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f)),
                    new ShaderProgramConfig.Uniform("ProjMat", "matrix4x4", 16, List.of(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f)),
                    new ShaderProgramConfig.Uniform("TextureMat", "matrix4x4", 16, List.of(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f)),

                    new ShaderProgramConfig.Uniform("ScreenSize", "float", 2, List.of(0f, 0f)),
                    new ShaderProgramConfig.Uniform("GameTime", "float", 1, List.of(0f)),

                    new ShaderProgramConfig.Uniform("FogStart", "float", 1, List.of(0f)),
                    new ShaderProgramConfig.Uniform("FogEnd", "float", 1, List.of(1f)),
                    new ShaderProgramConfig.Uniform("FogColor", "float", 4, List.of(0f,0f,0f,0f)),
                    new ShaderProgramConfig.Uniform("FogShape", "int", 1, List.of(0f)),

                    new ShaderProgramConfig.Uniform("Light0_Direction", "float", 3, List.of(0f, 0f, 0f)),
                    new ShaderProgramConfig.Uniform("Light1_Direction", "float", 3, List.of(0f, 0f, 0f)),

                    new ShaderProgramConfig.Uniform("ColorModulator", "float", 4, List.of(1f, 1f, 1f, 1f)),
                    new ShaderProgramConfig.Uniform("LineWidth", "float", 1, List.of(1f)),
                    new ShaderProgramConfig.Uniform("GlintAlpha", "float", 1, List.of(1f)),
                    new ShaderProgramConfig.Uniform("ModelOffset", "float", 3, List.of(0f, 0f, 0f)),

                    new ShaderProgramConfig.Uniform("FiguraRootMatrix", "matrix4x4", 16, List.of(1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f,0f,0f,0f,0f,1f))
            ), List.of(
                    new ShaderProgramConfig.Sampler("Sampler0"),
                    new ShaderProgramConfig.Sampler("Sampler1")
            ));
            return program;
        } catch (ShaderManager.CompilationException ex) {
            throw new IllegalStateException("Figura failed to compile optimized shader \"" + name + "\"", ex);
        }
    }

    private static final String VERSION_HEADER = """
            #version 460
            """;

    private static final String UNIFORMS = """
            // Most of these uniforms are unused by entities, but might as well add them.
            
            // Matrices
            uniform mat4 ModelViewMat; // Sends world -> view
            uniform mat4 ProjMat; // Sends view -> clip
            uniform mat4 TextureMat;
            
            // Generic useful stuff
            uniform vec2 ScreenSize;
            uniform float GameTime;
            
            // Fog
            uniform float FogStart;
            uniform float FogEnd;
            uniform vec4 FogColor;
            uniform int FogShape;
            
            // Light
            uniform vec3 Light0_Direction;
            uniform vec3 Light1_Direction;
            
            // Misc
            uniform vec4 ColorModulator;
            uniform float LineWidth;
            uniform float GlintAlpha;
            uniform vec3 ModelOffset;
            """;

    // Common VSH code
    private static final String VSH_HEADER = VERSION_HEADER + UNIFORMS + """
            
            // Weighted vertex format
            in vec3 Position;
            in vec3 Normal;
            in vec2 UV;
            in ivec2 PartID_1;
            in ivec2 PartID_2;
            in vec4 Weights;
            
            // Figura Defined
            uniform mat4 FiguraRootMatrix; // Custom matrix uniform, sends model -> world
            
            struct PartData {
                mat4 transform; // Position transform for the part
                mat3 normalMat; // Normal matrix for the part
                vec4 color; // Color multiplier for the part
            };
            layout(binding = 0, std430) readonly buffer PartDataBuffer {
                PartData parts[];
            };
            
            
            // Compute weighted values according to the matrices.
            // Also transforms the pos/normal by the FiguraRootMatrix.
            void figura_compute_weights(out vec4 pos, out vec3 normal, out vec4 color) {
                pos = vec4(0.0);
                normal = vec3(0.0);
                color = vec4(0.0);
                if (PartID_1.x != -1) {
                    PartData part = parts[PartID_1.x];
                    pos += Weights.x * part.transform * vec4(Position, 1.0);
                    normal += Weights.x * part.normalMat * Normal;
                    color += Weights.x * part.color;
                }
                if (PartID_1.y != -1) {
                    PartData part = parts[PartID_1.y];
                    pos += Weights.y * part.transform * vec4(Position, 1.0);
                    normal += Weights.y * part.normalMat * Normal;
                    color += Weights.y * part.color;
                }
                if (PartID_2.x != -1) {
                    PartData part = parts[PartID_2.x];
                    pos += Weights.z * part.transform * vec4(Position, 1.0);
                    normal += Weights.z * part.normalMat * Normal;
                    color += Weights.z * part.color;
                }
                if (PartID_2.y != -1) {
                    PartData part = parts[PartID_2.y];
                    pos += Weights.w * part.transform * vec4(Position, 1.0);
                    normal += Weights.w * part.normalMat * Normal;
                    color += Weights.w * part.color;
                }
                pos = FiguraRootMatrix * pos;
                normal = normalize((FiguraRootMatrix * vec4(normal, 0.0)).xyz);
            }
            """;

    // Includes used by MC
    private static final String FOG = """
            vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
                if (vertexDistance <= fogStart) {
                    return inColor;
                }
                float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
                return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
            }

            float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
                if (vertexDistance <= fogStart) {
                    return 1.0;
                } else if (vertexDistance >= fogEnd) {
                    return 0.0;
                }
                return smoothstep(fogEnd, fogStart, vertexDistance);
            }

            float fog_distance(vec3 pos, int shape) {
                if (shape == 0) {
                    return length(pos);
                } else {
                    float distXZ = length(pos.xz);
                    float distY = abs(pos.y);
                    return max(distXZ, distY);
                }
            }
            """;
    private static final String LIGHT = """
            #define MINECRAFT_LIGHT_POWER   (0.6)
            #define MINECRAFT_AMBIENT_LIGHT (0.4)

            vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {
                float light0 = max(0.0, dot(lightDir0, normal));
                float light1 = max(0.0, dot(lightDir1, normal));
                float lightAccum = min(1.0, (light0 + light1) * MINECRAFT_LIGHT_POWER + MINECRAFT_AMBIENT_LIGHT);
                return vec4(color.rgb * lightAccum, color.a);
            }

            vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
                return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
            }
            """;


    // Vertex shader for the "Basic" render type
    private static final String BASIC_VSH = VSH_HEADER + FOG + LIGHT + """
            
            // Define out variables for FSH:
            out float vertDistance; // Distance of vertex from camera
            out vec2 vertUV; // UV of the vertex
            out vec4 vertColor; // Color multiplier for the vertex
            out vec3 vertWorldSpaceNormal; // World-space normal of the vertex, normalized
            
            void main() {
                // Compute values
                vec4 pos;
                vec3 normal;
                vec4 color;
                figura_compute_weights(pos, normal, color);
            
                // Store and pass to frag shader
                gl_Position = ProjMat * ModelViewMat * pos;
                vertDistance = fog_distance(pos.xyz, FogShape);
                vertUV = UV;
                vertColor = color;
                vertWorldSpaceNormal = normal;
            }
            
            """;
    private static final String BASIC_FSH = VERSION_HEADER + UNIFORMS + LIGHT + FOG + """
            
            // In variables from vert shader:
            in float vertDistance;
            in vec2 vertUV;
            in vec4 vertColor;
            in vec3 vertWorldSpaceNormal;
            
            // Samplers...
            uniform sampler2D Sampler0;
            uniform sampler2D Sampler1;
            
            // Out color
            out vec4 fragColor;
            
            void main() {
                vec4 color = texture(Sampler0, vertUV);
                if (color.a == 0.0) { discard; }
                vec4 litVertColor = minecraft_mix_light(Light0_Direction, Light1_Direction, normalize(vertWorldSpaceNormal), vertColor);
                color *= litVertColor * ColorModulator;
                // TODO overlay and lightmap!!
                fragColor = linear_fog(color, vertDistance, FogStart, FogEnd, FogColor);
            }
            
            """;

    public static final CompiledShaderProgram BASIC = createOptimizedShader("basic", BASIC_VSH, BASIC_FSH);

    public static final BiFunction<@NotNull ResourceLocation, @NotNull ResourceLocation, RenderType> OPTIMIZED_BASIC = Util.memoize((mainTex, emissiveTex) -> {
        CompositeState compositeState = CompositeState.builder()
                .setColorLogicState(new CompiledShaderShard(BASIC))
                .setTextureState(MultiTextureStateShard.builder()
                        .add(mainTex, false, false)
                        .add(emissiveTex, false, false)
                        .build())
                .createCompositeState(true);
        return RenderType.create("figura_optimized_basic", FORMAT, VertexFormat.Mode.QUADS, 4096, compositeState);
    });

    // A bit cursed to use ColorLogicStateShard, but it's only currently ever used for text rendering,
    // and the alternatives are much more invasive.
    private static class CompiledShaderShard extends RenderStateShard.ColorLogicStateShard {
        public CompiledShaderShard(CompiledShaderProgram program) {
            super("figura_shader", () -> {
                NO_COLOR_LOGIC.setupRenderState(); // Make sure to use the original
                RenderSystem.setShader(program); // Set shader to compiled program
            }, () -> {});
        }
    }

}
