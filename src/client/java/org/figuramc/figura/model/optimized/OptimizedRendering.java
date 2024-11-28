package org.figuramc.figura.model.optimized;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.figuramc.figura.util.exception.ExceptionUtils;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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

    public static final ShaderInstance OPTIMIZED_SHADER = ExceptionUtils.tryRun(() -> new ShaderInstance(new ResourceProvider() {
        int index = 0;
        @Override
        public Optional<Resource> getResource(ResourceLocation resourceLocation) {
            return Optional.of(switch (index++) {
                // Shader json
                case 0 -> new Resource(null, null) {
                    @Override public InputStream open() {
                        String s = """
                                {
                                    "vertex": "doesntmatter",
                                    "fragment": "doesntmatter",
                                    "samplers": [
                                        { "name": "Sampler0" }
                                    ],
                                    "uniforms": [
                                        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                                        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                                        { "name": "Light0_Direction", "type": "float", "count": 3, "values": [0.0, 0.0, 0.0] },
                                        { "name": "Light1_Direction", "type": "float", "count": 3, "values": [0.0, 0.0, 0.0] },
                                
                                        { "name": "FiguraRootMatrix", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] }
                                    ]
                                }
                                """;
                        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
                    }
                };
                // Vertex shader source
                case 1 -> new Resource(null, null) {
                    @Override public InputStream open() {
                        String s = """
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
                        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
                    }
                    @Override public String sourcePackId() { return "Figura Optimized Vertex Shader"; }
                };
                // Fragment shader source
                case 2 -> new Resource(null, null) {
                    @Override public InputStream open() {
                        String s = """
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
                        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
                    }
                    @Override public String sourcePackId() { return "Figura Optimized Fragment Shader"; }
                };
                default -> throw new IllegalStateException("Invalid shader creation process - Something is incompatible with Figura!");
            });
        }
    }, "", OPTIMIZED_FORMAT), IllegalStateException::new);

    public static final Function<ResourceLocation, RenderType> OPTIMIZED_RENDER_TYPE = Util.memoize(texture -> {
        CompositeState compositeState = CompositeState.builder()
                .setShaderState(new ShaderStateShard(() -> OPTIMIZED_SHADER))
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .createCompositeState(true);
        return RenderType.create("figura_optimized", OPTIMIZED_FORMAT, VertexFormat.Mode.QUADS, 4096, compositeState);
    });

}
