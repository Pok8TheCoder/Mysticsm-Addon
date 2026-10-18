package io.github.pok8.mysticismultimates.entity;

public enum SkillAnimationVariant {
    ANTAEUS_BLUE("antaeus_blue", "geo/antaeus_blue_projectile.geo.json", "textures/entity/skill/antaeus_blue.png", "animations/antaeus_blue_projectile.animation.json", "animation.antaeus_blue_projectile.idle", true),
    ANTAEUS_RED("antaeus_red", "geo/antaeus_red_projectile.geo.json", "textures/entity/skill/antaeus_red.png", "animations/antaeus_red_projectile.animation.json", "animation.antaeus_red_projectile.idle", true),
    ANTAEUS_PURPLE("antaeus_purple", "geo/antaeus_purple_projectile.geo.json", "textures/entity/skill/antaeus_purple.png", "animations/antaeus_purple_projectile.animation.json", "animation.antaeus_purple_projectile.idle", true),
    GATES("gates", "geo/gates.geo.json", "textures/entity/skill/gates.png", "animations/gates.animation.json", "Gate rotation", false),
    EA("ea", "geo/weapons/ea.geo.json", "textures/item/ea.png", "animations/weapons/ea.animation.json", "Rotations", false),
    BUBBLE_SHIELD("bubble_shield", "geo/bubble_shield.geo.json", "textures/entity/skill/bubble_shield.png", "animations/bubble_shield_entity.animation.json", "Idle", false),
    DISPENSER("dispenser", "geo/dispenser.geo.json", "textures/entity/dispenser/dispenser.png", "animations/dispenser.animation.json", "idle", false),
    DISPENSER_2("dispenser_2", "geo/dispenser_2.geo.json", "textures/entity/dispenser/dispenser.png", "animations/dispenser_2.animation.json", "idle", false),
    DISPENSER_3("dispenser_3", "geo/dispenser_3.geo.json", "textures/entity/dispenser/dispenser.png", "animations/dispenser_3.animation.json", "idle", false),
    MISSILE("missile", "geo/missile.geo.json", "textures/entity/skill/missile.png", "animations/missile_shooting.json", "missile.shoot", true),
    RADIANT_LION("radiant_lion", "geo/radiant_lion.geo.json", "textures/entity/radiant_lion.png", "animations/radiant_lion.json", "Walk", false),
    SENTRY_1("sentry_1", "geo/sentry_1.geo.json", "textures/entity/sentry/sentry_1.png", "animations/sentry_1.anim.json", "sentry5.shoot", false),
    SENTRY_2("sentry_2", "geo/sentry_2.geo.json", "textures/entity/sentry/sentry_2.png", "animations/sentry_2.anim.json", "sentry5.shoot", false),
    SENTRY_3("sentry_3", "geo/sentry_3.geo.json", "textures/entity/sentry/sentry_3.png", "animations/sentry_3.anim.json", "sentry5.shoot", false),
    SENTRY_4("sentry_4", "geo/sentry_4.geo.json", "textures/entity/sentry/sentry_4.png", "animations/sentry_4.anim.json", "sentry4.shoot", false),
    SENTRY_5("sentry_5", "geo/sentry_5.geo.json", "textures/entity/sentry/sentry_5.png", "animations/sentry_5.anim.json", "sentry5.shoot", false),
    SHADOW_BALL("shadow_ball", "geo/shadow_ball.geo.json", "textures/entity/shadow_ball.png", "animations/shadow_ball.animation.json", "animation.model.idle", true),
    SHADOW_IMP("shadow_imp", "geo/shadow_imp.geo.json", "textures/entity/shadow_imp.png", "animations/shadow_imp.animation.json", "animation.shadow_imp.idle", false);

    private final String id;
    private final String model;
    private final String texture;
    private final String animationFile;
    private final String animation;
    private final boolean projectile;

    SkillAnimationVariant(String id, String model, String texture, String animationFile, String animation, boolean projectile) {
        this.id = id;
        this.model = model;
        this.texture = texture;
        this.animationFile = animationFile;
        this.animation = animation;
        this.projectile = projectile;
    }

    public String id() {
        return id;
    }

    public String model() {
        return model;
    }

    public String texture() {
        return texture;
    }

    public String animationFile() {
        return animationFile;
    }

    public String animation() {
        return animation;
    }

    public boolean projectile() {
        return projectile;
    }

    public int lifetime() {
        return projectile ? 40 : 160;
    }
}
