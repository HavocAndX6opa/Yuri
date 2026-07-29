package ddlc.yuri.api.events.impl.render;

import ddlc.yuri.api.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Shader2DEvent implements Event {
    private ShaderType shaderType;

    public Shader2DEvent(ShaderType shaderType) {
        this.shaderType = shaderType;
    }

    // not doing bloom because it's a bit more complicated and I don't want to deal with it right now

    public enum ShaderType {
        BLUR, SHADOW
    }
}