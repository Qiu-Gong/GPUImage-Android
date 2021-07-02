package com.alien.gpuimage.filter

class MaskBlendFilter : TwoInputFilter(fragmentShader = SHADER_STRING) {
    companion object {
        private const val SHADER_STRING =
            """
            varying highp vec2 textureCoordinate;
            varying highp vec2 textureCoordinate2;
            
            uniform sampler2D inputImageTexture;
            uniform sampler2D inputImageTexture2;
            
            void main()
            {
                lowp vec4 base = texture2D(inputImageTexture, textureCoordinate);
                lowp vec4 overlay = texture2D(inputImageTexture2, textureCoordinate2);
                
                gl_FragColor = vec4(base.r * overlay.r, base.g * overlay.g, base.b * overlay.b, 1);
            }
            """
    }
}