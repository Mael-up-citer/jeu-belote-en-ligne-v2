package src.main;

import src.main.Paquet.Carte;



/**
 * Classe représentant un bot intermédiaire.
 */
class BotMoyen extends Bot {
    public BotMoyen(String nom) {
        super(nom);
    }


    @Override
    public Paquet.Carte jouer(Plis plis) {
        final int DEEPTH = 5;   // Profondeur dans l'arbre de recherche

        //System.out.println("\nJeu du bot Débutant: "+ main);
        //System.out.println("\n"+getNom() + " (Débutant) joue, il a le choix avec "+ Rules.playable(plis, this));

        // L'IA donne une carte
        Carte carte = exceptedMiniMaxAlphaBeta(plis, noPlayer, DEEPTH);

        System.out.println("carte joué: "+carte);

        // L'ajoute dans le plis
        plis.addCard(this, carte);

        // L'enlève de la main
        removeCarte(carte);

        return carte;
    }
}