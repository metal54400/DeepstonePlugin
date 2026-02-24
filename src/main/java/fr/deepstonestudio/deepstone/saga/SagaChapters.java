package fr.deepstonestudio.deepstone.saga;

import java.util.List;

public final class SagaChapters {
    private SagaChapters() {}

    public static final List<List<String>> CHAPTERS = List.of(

            List.of(
                    "&8— &6Saga I &8— &eLe Sang des Anciens",
                    "&7Selon les sagas, Ragnar descend d’une lignée noble.",
                    "&7On murmure que son père était un chef respecté,",
                    "&7et que sa mère portait la sagesse des anciens dieux.",
                    "&bDès sa naissance, le destin semblait l’attendre."
            ),

            List.of(
                    "&8— &6Saga II &8— &eL’Ambition",
                    "&7Ragnar ne voulait pas être fermier toute sa vie.",
                    "&7Il rêvait de terres au-delà de l’horizon.",
                    "&b« Pourquoi rester petit… quand le monde est immense ? »",
                    "&7Son ambition brûlait plus fort que la peur."
            ),

            List.of(
                    "&8— &6Saga III &8— &eSes Qualités",
                    "&7Il était rusé, intelligent et audacieux.",
                    "&7Il écoutait ses ennemis avant de les frapper.",
                    "&7Il inspirait ses hommes par la vision d’un futur nouveau.",
                    "&6Un chef né pour guider."
            ),

            List.of(
                    "&8— &6Saga IV &8— &eSes Défauts",
                    "&7Mais l’ambition l’aveuglait.",
                    "&7Il devenait fier, parfois cruel.",
                    "&7Il sacrifia des liens pour poursuivre la gloire.",
                    "&cLa solitude devint son ombre."
            ),

            List.of(
                    "&8— &6Saga V &8— &eRollo, le Frère",
                    "&7À ses côtés marchait son frère : &eRollo&7.",
                    "&7Puissant, impulsif… toujours comparé à Ragnar.",
                    "&7Entre amour et rivalité, leur lien se fissura.",
                    "&cDeux loups ne peuvent partager la même couronne."
            ),

            List.of(
                    "&8— &6Saga VI &8— &eSes Épouses",
                    "&eLagertha&7, guerrière et égale.",
                    "&eThora&7, mentionnée dans certaines légendes.",
                    "&eAslaug&7, femme de prophétie.",
                    "&7Chacune marqua sa vie… et son destin."
            ),

            List.of(
                    "&8— &6Saga VII &8— &eGyda",
                    "&7Sa fille &eGyda&7 fut sa lumière.",
                    "&7Ragnar l’aimait plus que l’or et la gloire.",
                    "&7Mais même les rois ne contrôlent pas le destin.",
                    "&7Sa perte creusa une faille dans son cœur."
            ),

            List.of(
                    "&8— &6Saga VIII &8— &eSes Fils",
                    "&eBjörn&7, l’explorateur indomptable.",
                    "&eUbbe&7, le réfléchi et loyal.",
                    "&eHvitserk&7, le tourmenté.",
                    "&eIvar&7, le stratège redoutable.",
                    "&7Ragnar les façonna pour dépasser son propre nom."
            ),

            List.of(
                    "&8— &6Saga IX &8— &eSes Proches",
                    "&7Floki, le constructeur visionnaire.",
                    "&7Ses guerriers fidèles, prêts à mourir pour lui.",
                    "&7Mais la confiance est fragile quand la gloire grandit."
            ),

            List.of(
                    "&8— &6Saga X &8— &eSes Ennemis",
                    "&7Rois saxons et chefs jaloux le craignaient.",
                    "&7Les prêtres le maudissaient.",
                    "&7Chaque victoire attirait plus d’ennemis.",
                    "&cPlus il montait… plus sa chute approchait."
            ),

            List.of(
                    "&8— &6Saga XI &8— &eLa Chute",
                    "&7Les échecs s’accumulèrent.",
                    "&7Ses hommes doutèrent.",
                    "&7Son nom, autrefois glorieux, devint controversé.",
                    "&7Le roi fatigué n’était plus invincible."
            ),

            List.of(
                    "&8— &6Saga XII &8— &eLa Mort",
                    "&7Capturé par ses ennemis, Ragnar fit face au destin.",
                    "&7Jeté dans une fosse de serpents selon la légende,",
                    "&cIl ne cria pas.",
                    "&b« Comme les petits cochons vont grogner… »",
                    "&7Sa mort devint une étincelle."
            ),

            List.of(
                    "&8— &6Saga XIII &8— &eL’Héritage",
                    "&7Ses fils jurèrent vengeance.",
                    "&7Le monde trembla sous leur colère.",
                    "&6Ragnar était mort…",
                    "&7mais son nom devint éternel.",
                    "&eLa saga des Ragnarson ne faisait que commencer."
            )
    );

    public static int maxChapterIndex() {
        return CHAPTERS.size() - 1;
    }
}