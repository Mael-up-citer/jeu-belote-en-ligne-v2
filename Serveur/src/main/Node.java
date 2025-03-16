package src.main;



import java.util.HashMap;
import java.util.List;

import src.main.Paquet.Carte.Couleur;

class Node {
    private List<Node> succeseurs;  // List des successeurs
    private Plis plis;  // Plis en cours
    private int deep;   //  Profondeur du noeud

    // Contient toutes les cartes joué jusque la
    private HashMap<Couleur, List<Paquet.Carte>> cartePlay = new HashMap<>();

    Node() {}

    Node(int deep) {
        this.deep = deep;
    }
}