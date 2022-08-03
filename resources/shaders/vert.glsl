#version 450

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texcoord;

layout(push_constant) uniform matrices {
    mat4 projectionMatrix;
    mat4 viewMatrix;
    mat4 modelMatrix;
} push_constants;

layout(location = 0) out vec2 texCoord;

void main()
{
    gl_Position = push_constants.projectionMatrix * push_constants.viewMatrix * push_constants.modelMatrix * vec4(position, 1);
    texCoord = texcoord;
}