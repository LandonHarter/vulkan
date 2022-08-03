#version 450

layout(location = 0) out vec4 fragColor;

layout(location = 0) in vec2 texCoord;

void main()
{
    fragColor = vec4(texCoord, 0, 1);
}