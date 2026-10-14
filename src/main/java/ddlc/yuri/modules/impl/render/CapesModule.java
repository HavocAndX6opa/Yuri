package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.util.ResourceLocation;

@ModuleInfo(label = "Capes", description = "Allows you to change your cape", category = ModuleCategory.RENDER)
public class CapesModule extends Module {
    public static ModeProperty<Cape> cape = new ModeProperty<>("Cape", Cape.YURI);

    public enum Cape {
        YURI("Yuri"), NATSUKI("Natsuki"), ASTOLFO("Astolfo"), PURPLE_GIRL("Purple Girl"), SATAN("Satan"), ROSE("Rose"), PULSIVE("Pulsive"), SAD("Sad"), ZERO_TWO("Zero Two"), SKY("Sky"),
        ANIME1("Anime1"), BUTTON("Button"), CAT("Cat"), EGIRL("Egirl"), GIRL2("Girl2"), GOTH("Goth"), HENTI2("Henti2"), MOMMY("Mommy"), MONSTER("Monster"), SABRINA("Sabrina"), VAPE("Vape");

        public final String name;

        Cape(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        /**
         * The texture this cape binds. The display name doubles as the file name, so "Zero Two"
         * needs a zero_two.png beside it - keep any new entry's name matching its file.
         */
        public ResourceLocation texture() {
            return new ResourceLocation("yuri/capes/" + this.name.toLowerCase().replace(" ", "_") + ".png");
        }
    }
}