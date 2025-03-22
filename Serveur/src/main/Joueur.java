package src.main;

import src.main.Paquet.Carte;
import src.main.Paquet.Carte.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


import java.io.IOException;


/**
 * Classe abstraite représentant un joueur, qu'il soit humain ou un bot.
 */
public abstract class Joueur {
    public static Paquet.Carte.Couleur colorAtout;  // Atout de la couleur

    protected Equipe equipe; // L'équipe à laquelle appartient le joueur
    protected String nom; // Le nom du joueur
    protected HashMap<Paquet.Carte.Couleur, List<Paquet.Carte>> main; // La main du joueur, organisée par couleur
    protected int noPlayer; // Nuémro du jour

    /**
     * Constructeur par défaut de la classe Joueur.
     */
    public Joueur() {
        this.nom = "Joueur inconnu";
        initMain();
    }

    /**
     * Constructeur de la classe Joueur avec un nom.
     *
     * @param nom Le nom du joueur.
     */
    public Joueur(String nom) {
        this.nom = nom;
        initMain();
    }

    /**
     * Initialise la main du joueur avec les couleurs de cartes disponibles.
     */
    private void initMain() {
        this.main = new HashMap<>();
        for (Paquet.Carte.Couleur couleur : Paquet.Carte.Couleur.values())
            main.put(couleur, new ArrayList<>());
    }

    // Vide les listes de cartes
    public void clearMain() {
        for (List<Carte> list : main.values()) list.clear();
    }

    /**
     * Retourne le nom du joueur.
     *
     * @return Le nom du joueur.
     */
    public String getNom() {
        return nom;
    }

    /**
     * Retourne l'équipe du joueur.
     *
     * @return L'équipe du joueur.
     */
    public Equipe getEquipe() {
        return equipe;
    }

    /**
     * Définit l'équipe du joueur.
     *
     * @param equipe L'équipe à assigner au joueur.
     */
    public void setEquipe(Equipe equipe) {
        this.equipe = equipe;
    }

    /**
     * Ajoute une carte à la main du joueur.
     *
     * @param carte La carte à ajouter.
     */
    public void addCard(Paquet.Carte carte) {
        Carte.Couleur key = carte.getCouleur();
        if (main.get(carte.getCouleur()) == null)
            throw new IllegalStateException("Erreur la clef: "+key+"    N'est pas défini ici");

            main.get(key).add(carte);
    }

    /**
     * Trie les cartes de la main du joueur par ordre croissant de valeur.
     */
    public void sortCard() {
        main.values().forEach(cartes -> Collections.sort(cartes));
    }

    /**
     * Méthode abstraite définissant l'action de jouer un tour.
     */
    public abstract Paquet.Carte jouer(Plis plis);

    // Supprime la carte c de la main du joueur
    protected void removeCarte(Carte c) throws IllegalArgumentException {
        if (c == null || main.get(c.getCouleur()) == null)
            throw new IllegalArgumentException("Aucun carte de cette couleur n'hésiste "+c);

        main.get(c.getCouleur()).remove(c);
    }

    /**
     * Méthode définissant l'action à réaliser pour choisir l'atout.
     */
    public abstract Paquet.Carte.Couleur parler(int tour);


    public HashMap<Paquet.Carte.Couleur, List<Paquet.Carte>> getMain() {
        return main;
    }

    public int getNoPlayer() {
        return noPlayer;
    }

    public void setNoPlayer(int no) {
        noPlayer = no;
    }
}

/**
 * Classe représentant un joueur humain.
 */
class Humain extends Joueur {
    private Socket socket; // Socket pour la communication avec le client (non sérialisé)
    private PrintWriter out;
    private BufferedReader in;


    /**
     * Constructeur d'un joueur humain avec une connexion réseau.
     *
     * @param socket La socket utilisée pour la communication réseau.
     */
    public Humain(Socket socket, BufferedReader in, PrintWriter out) {
        super("Joueur inconnu");
        this.socket = socket;
    }

    /**
     * Constructeur d'un joueur humain avec un nom et une connexion réseau.
     *
     * @param nom    Le nom du joueur.
     * @param socket La socket utilisée pour la communication réseau.
     */
    public Humain(String nom, Socket socket, BufferedReader in, PrintWriter out) {
        super(nom);
        this.socket = socket;
        this.in = in;
        this.out = out;
    }
 
    /**
     * Joue un tour en interagissant avec le client via le réseau.
     */
    @Override
    public Paquet.Carte.Couleur parler(int tour) {
        System.out.println("humain parle");
        // Previens le clients qu'on attend qu'il donne un atout
        notifier("GetAtout"+tour+":$");

        // Récupère sa réponse sous forme d'une des valeurs de l'enum couleur ou 'Passer'
        String atout = waitForClient();

        if (atout.equals("Passer")) return null;    // Si le joueur ne prend pas

        try {   // Récupère la couleur
            Paquet.Carte.Couleur res = Paquet.Carte.Couleur.valueOf(atout.toUpperCase());
            return res;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Joue un tour en interagissant avec le client via le réseau.
     */
    @Override
    public Paquet.Carte jouer(Plis plis) {
        // Previens le clients qu'on attend qu'il pose une carte
        notifier("Play:"+Rules.playable(plis, this));

        // Récupère sa réponse sous forme: "TypeDeCouleur"
        String cartePlaye = waitForClient();

       // Récupère la carte joué
        Paquet.Carte carte = Paquet.Carte.parseCarte(cartePlaye);

        System.out.println("Le joueur "+nom+"  a joué la carte "+carte);
        System.out.println("main du joueur =  "+main);

        // L'enlève de la main
        removeCarte(carte);

        // L'ajoute dans le plis
        plis.addCard(this, carte);

        return carte;
    }

    public String waitForClient() {
        // Code bloquant pour attendre un message entrant du client
        try {
            // Lire une ligne du flux d'entrée
            String message = in.readLine();
            // Si le message reçu est nul, cela signifie que la connexion a été fermée par le client
            if (message == null) {
                System.err.println("Le client a fermé la connexion.");
                return null;  // Retourne null si le client a déconnecté
            }

            // Si un message est reçu, le retourner
            return message;
        } catch (IOException e) {
            // Capture l'exception d'entrée/sortie
            System.err.println("Erreur lors de la lecture du message du client.");
            e.printStackTrace();
        } catch (Exception e) {
            // Capture d'autres erreurs générales
            System.err.println("Une erreur inattendue s'est produite.");
            e.printStackTrace();
        }
        return null;  // Retourne null en cas d'erreur
    }    

    /**
     * Notifie le joueur avec un message.
     *
     * @param message Message à envoyer au joueur.
     */
    public void notifier(String message) {
        if (out != null)
            out.println(message);  // Envoie le message au client
        else
            System.err.println("Erreur: le flux de sortie est null.");
    }

    /**
     * Termine la connexion du joueur humain en fermant les flux et la socket.
     */
    public void endConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la fermeture de la connexion pour " + getNom());
        }
    }

    /**
     * Retourne la socket associée au joueur humain.
     *
     * @return La socket du joueur.
     */
    public Socket getSocket() {
        return socket;
    }
}



/*
 * ************************************************************************
 * Partie des bots / IA
 * ************************************************************************
 */


class BotFactory {
    private static final Map<String, Function<String, Joueur>> BOT_OF = new HashMap<>();

    static {
        BOT_OF.put("débutant", BotDebutant::new);
        BOT_OF.put("intermédiaire", BotMoyen::new);
        BOT_OF.put("expert", BotExpert::new);
    }

    /**
     * Crée un bot avec un nom personnalisé et un niveau spécifié.
     *
     * @param nom    Le nom du bot.
     * @param niveau Le niveau du bot.
     * @return Une instance du bot correspondant.
     */
    public static Joueur creeBot(String nom, String niveau) {
        // Extrait la fonction associé
        Function<String, Joueur> botSupplier = BOT_OF.get(niveau.toLowerCase());
        if (botSupplier == null)    // Si aucune ne correspond
            throw new IllegalArgumentException("Erreur, la difficulté: " + niveau + " n'est pas connue");

        return botSupplier.apply(nom);  // Retourne une nouvelle instance
    }
}

abstract class Bot extends Joueur {
    // Map qui associe à chaque couleur un score dans le cadre de la prise à l'atout
    protected Map<Paquet.Carte.Couleur, Integer> atoutRate = new HashMap<>();

    // Liste de Map qui associe à chaque cartes sa proba pour tous les joueurs
    public static Map<Integer, Map<Couleur, Map<Carte, Float>>> cardsProbaPerPlayer;


    Bot (String name) {
        super(name);

        cardsProbaPerPlayer = new HashMap<>();
    }


    public static void endDistrib() {
        // Remplit les maps en attribuant une probabilité initiale de 1/4
        // à chaque joueur pour posséder chaque carte, en préservant l'ordre d'insertion
        for (int i = 0; i < Game.NB_PLAYERS; i++) {
            cardsProbaPerPlayer.put(i, new LinkedHashMap<>());
            for (Couleur c : Couleur.values()) {
                cardsProbaPerPlayer.get(i).put(c, new LinkedHashMap<>());
                // L'ordre ordinal de l'énumération est conservé par Type.values()
                for (Type t : Type.values()) {
                    Carte carte = new Carte(c, t);
                    cardsProbaPerPlayer.get(i).get(c).put(carte, 0.25f);
                }
            }
        }
        System.out.println(cardsProbaPerPlayer);
    }
    

    @Override
    public Couleur parler(int tour) {
        // Si la main est vide, on passe
        if (main == null || main.isEmpty()) return null;

        final int seuil = 85;     // Défini à partir de quelle score on peut prendre
        final int seuilHaut = 115; // Défini à partir de quelle score on peut prendre en fin de partie
        Couleur color = null;   // Défini la couleur de l'atout choisit ou null si on passe

        if (atoutRate.isEmpty()) {
            // On évalue la main pour chaque couleur
            for (Paquet.Carte.Couleur couleur : main.keySet()) {
                System.out.println("score de "+couleur+" = "+evaluerScore(couleur));
                atoutRate.put(couleur, evaluerScore(couleur));
            }

            if (getEquipe().getScore() > 800 && atoutRate.get(colorAtout) > seuilHaut) color = colorAtout;
            else if (atoutRate.get(colorAtout) > seuil) color = colorAtout;
        }
        else {
            Couleur verif = betterRate();

            if ((getEquipe().getScore() > 800 && atoutRate.get(verif) > seuilHaut)
                        ||
            (atoutRate.get(verif) > seuil))
                color = verif;
        }
        atoutRate.clear();
        return color;
    }


    private int evaluerScore(Paquet.Carte.Couleur couleur) {
        // On simule que la couleur passée en paramètre est l'atout
        couleur.setIsAtout(true);

        int score = calculerScoreAtout(main) +
                    calculerScoreMaitresses(main) +
                    calculerBonusBelote(main) +
                    calculerBonusLongueEtCoupe(main);
        
        // Rétablir l'état initial de la couleur atout
        couleur.setIsAtout(false);
        return score;
    }


    private int calculerScoreAtout(HashMap<Couleur, List<Carte>> main) {
        final int POIDS_NB_ATOUT = 5;
        final float POIDS_TOTAL_POWER_ATOUT = 1.66f;

        int nbAtouts = main.get(colorAtout).size();
        int totalPowerAtout = main.get(colorAtout)
                                  .stream()
                                  .mapToInt(Paquet.Carte::getNbPoint)
                                  .sum();
        
        return POIDS_NB_ATOUT * nbAtouts + (int) (POIDS_TOTAL_POWER_ATOUT * totalPowerAtout);
    }


    private int calculerScoreMaitresses(HashMap<Couleur, List<Carte>> main) {
        final int POIDS_NB_MAITRESSE = 3;
        final int POIDS_TOTAL_POWER_MAITRESSE = 1;

        int nbMaitresses = 0;
        int totalPowerMaitresses = 0;

        // Parcours de toutes les couleurs sauf l'atout
        for (Map.Entry<Paquet.Carte.Couleur, List<Paquet.Carte>> entry : main.entrySet()) {
            Paquet.Carte.Couleur couleur = entry.getKey();
            if (!couleur.equals(colorAtout)) {
                List<Paquet.Carte> cartes = entry.getValue();
                int ordinalGradient = Carte.Type.AS.ordinal();
                int i = cartes.size() - 1;

                while (i >= 0 && ordinalGradient == cartes.get(i).getType().ordinal()) {
                    ordinalGradient--;
                    nbMaitresses++;
                    totalPowerMaitresses += cartes.get(i).getNbPoint();
                    i--;
                }
            }
        }
        return POIDS_NB_MAITRESSE * nbMaitresses + POIDS_TOTAL_POWER_MAITRESSE * totalPowerMaitresses;
    }


    private int calculerBonusBelote(HashMap<Couleur, List<Carte>> main) {
        final int BONUS_BELOTE = 20;
        boolean roi = false, dame = false;
 
        for (Paquet.Carte carte : main.get(colorAtout)) {
            if (carte.getType() == Carte.Type.ROI) roi = true;
            else if (carte.getType() == Carte.Type.DAME) dame = true;
        }
        return (roi && dame) ? BONUS_BELOTE : 0;
    }
    
    /**
     * Calcule en une seule passe la somme des bonus :
     * - BONUS_COUPE si une couleur ne contient aucune carte.
     * - BONUS_LONGUE si la couleur contient au moins SEUIL_LONGUE cartes maîtresses,
     *   avec un bonus de base et un bonus incrémental par carte supplémentaire.
     */
    private int calculerBonusLongueEtCoupe(HashMap<Couleur, List<Carte>> main) {
        final int BONUS_COUPE = 10;
        final int BONUS_LONGUE_DE_BASE = 20;
        final int BONUS_LONGUE_INCREMENT = 10;
        final int SEUIL_LONGUE = 2;
        
        int bonusTotal = 0;
        
        // Parcours unique de toutes les couleurs de la main
        for (List<Paquet.Carte> cartes : main.values()) {
            if (cartes.isEmpty()) bonusTotal += BONUS_COUPE;

            else {
                int ordinalGradient = Carte.Type.AS.ordinal();
                int i = cartes.size() - 1;
                int cptLongue = 0;

                while (i >= 0 && ordinalGradient == cartes.get(i).getType().ordinal()) {
                    ordinalGradient--;
                    cptLongue++;
                    i--;
                }
                if (cptLongue >= SEUIL_LONGUE)
                    bonusTotal += BONUS_LONGUE_DE_BASE + (cptLongue - SEUIL_LONGUE) * BONUS_LONGUE_INCREMENT;
            }
        }
        return bonusTotal;
    }        


    // Retourne la couleur pour laquelle on a le plus de chance de réussir
    private Couleur betterRate() {
        int max = 0;
        Couleur res = null;

        for (Couleur couleur : atoutRate.keySet()) {
            if (couleur != colorAtout && atoutRate.get(couleur) > max) {
                max = atoutRate.get(couleur);
                res = couleur;
            }
        }
        return res;
    }


    // Implémentation de l'algorithme MiniMax pondéré par les probabilités
    protected Carte exceptedMiniMax(Plis plis, int noCurrentPlayeur, int maxDeepth) {
        // Récupère les cartes jouable par le joueur dans ce plis
        List<Carte> playable = Rules.playable(plis, this);
        Carte meilleureCarte = null;    // la carte qu'il faut jouer
        float meilleureValeur = -1; // Valeur du coup la meilleur carte

        // Parcourt chaque carte jouable
        for (Carte carte : playable) {
            System.out.println("simulation de: "+carte);

            // Clone le pli donné et simule le coup joué
            Plis p = new Plis(plis);
            p.addCard(this, carte);

            // Le set est initialisé avec la main du joueur
            Set<Carte> newCartesJouees = main.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

            // On ajoute aussi les cartes déjà jouées
            newCartesJouees.addAll(Game.cartePlay.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet()));

            // Ajoute la carte que l'on vient de jouer
            newCartesJouees.add(carte);

            // Calcul de la valeur de ce coup via le minValue pondéré
            float valeur = minValue(p, carte, (noCurrentPlayeur + 1) % Game.NB_PLAYERS, 0, maxDeepth, 0, newCartesJouees);

            if (valeur > meilleureValeur) {
                meilleureValeur = valeur;
                meilleureCarte = carte;
            }
        }
        return meilleureCarte;
    }


    private float minValue(Plis plis, Carte c, int noCurrentPlayeur, int deepth, int maxDeepth, float globalSum, Set<Carte> cartesJouees) {
        Plis modele;
        float localSum = globalSum;

        // Si le pli est fini, on réinitialise le modèle et on ajoute la valeur du pli si l'équipe du bot gagne
        if (plis.getIndex() == 4) {
            modele = new Plis(); // Nouveau pli

            if (plis.getEquipe().equals(equipe)) localSum += plis.getValue();
        }
        else modele = new Plis(plis);

        // Test terminal
        if (terminalTest(cartesJouees) || deepth == maxDeepth) return utility(localSum, cartesJouees);

        // Liste de toutes les cartes possibles dans le plis
        List<Carte> playable;

        // Si c'est le tour du joueur on calcul à partir de playable
        if (noCurrentPlayeur == noPlayer) playable = Rules.playable(plis, this);
        // Sinon on regarde toutes les possibilitées
        else playable = Rules.successeur(plis, noCurrentPlayeur);

        float minValeur = Float.POSITIVE_INFINITY;

        // Parcour toutes les cartes possibles
        for (Carte carte : playable) {
            System.out.println("\nMin simulation: "+carte);
            // Si la carte n'est pas déja joué
            if (!cartesJouees.contains(carte)) {
                Plis tmp = new Plis(modele);
                tmp.addCard(Game.joueurs[noCurrentPlayeur], carte);

                // Copie le set pour éviter de modifier l’original
                Set<Carte> newCartesJouees = new HashSet<>(cartesJouees);
                newCartesJouees.add(carte);

                // Récupère la probabilité associée à cette carte pour le joueur noCurrentPlayeur
                float proba = 0f;
                Map<Couleur, Map<Carte, Float>> probaParCouleur = cardsProbaPerPlayer.get(noCurrentPlayeur);
                if (probaParCouleur != null) {
                    Map<Carte, Float> probaCarte = probaParCouleur.getOrDefault(carte.getCouleur(), Collections.emptyMap());
                    proba = probaCarte.getOrDefault(carte, 0f);
                }

                float brancheValue = maxValue(tmp, carte, (noCurrentPlayeur + 1) % Game.NB_PLAYERS, deepth + 1, maxDeepth, localSum, newCartesJouees);
 
                // Pondération : plus la probabilité est faible, moins ce coup compte
                minValeur = Math.min(minValeur, brancheValue * proba);
            }
        }
        return minValeur;
    }


    private float maxValue(Plis plis, Carte c, int noCurrentPlayeur, int deepth, int maxDeepth, float globalSum, Set<Carte> cartesJouees) {
        Plis modele;
        float localSum = globalSum;

        // Si le pli est fini
        if (plis.getIndex() == 4) {
            modele = new Plis(); // Nouveau pli

            if (plis.getEquipe().equals(equipe)) localSum += plis.getValue();
        }
        else modele = new Plis(plis);

        if (terminalTest(cartesJouees) || deepth == maxDeepth) return utility(localSum, cartesJouees);

        // Liste de toutes les cartes possibles dans le plis
        List<Carte> playable;

        // Si c'est le tour du joueur on calcul à partir de playable
        if (noCurrentPlayeur == noPlayer) playable = Rules.playable(plis, this);
        // Sinon on regarde toutes les possibilitées
        else playable = Rules.successeur(plis, noCurrentPlayeur);

        float bestValeur = -1;

        for (Carte carte : playable) {
            System.out.println("\nMax simulation: "+carte);
            // Si la carte n'a pas été joué
            if (!cartesJouees.contains(carte)) {
                Plis tmp = new Plis(modele);
                tmp.addCard(Game.joueurs[noCurrentPlayeur], carte);

                Set<Carte> newCartesJouees = new HashSet<>(cartesJouees);
                newCartesJouees.add(carte);

                // Récupère la probabilité associée à cette carte pour le joueur noCurrentPlayeur
                float proba = 0f;
                Map<Couleur, Map<Carte, Float>> probaParCouleur = cardsProbaPerPlayer.get(noCurrentPlayeur);
                if (probaParCouleur != null) {
                    Map<Carte, Float> probaCarte = probaParCouleur.getOrDefault(carte.getCouleur(), Collections.emptyMap());
                    proba = probaCarte.getOrDefault(carte, 0f);
                }

                float brancheValue = minValue(tmp, carte, (noCurrentPlayeur + 1) % Game.NB_PLAYERS, deepth + 1, maxDeepth, localSum, newCartesJouees);
                bestValeur = Math.max(bestValeur, brancheValue * proba);
            }
        }
        return bestValeur;
    }


    // Test terminal : si toutes les cartes ont été jouées (par exemple, dans un jeu à 32 cartes)
    private boolean terminalTest(Set<Carte> cartesJouees) {
        System.out.println("terminal test: "+cartesJouees.size());
        return cartesJouees.size() == 32;
    }


    // Test de terminaison anticipé
    // la valeur du jeu + la nombre de pts accumulé, la valeur du jeu est null si on est dans le cas terminal
    private float utility(float globalSum, Set<Carte> cartesJouees) {
        // Copie complète de main avant les modifications
        HashMap<Couleur, List<Carte>> mainTmp = new HashMap<>(main.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> new ArrayList<>(e.getValue()) // Copie indépendante de chaque liste
        )));

        // Modifier la copie et non l'original
        for (Carte carte : cartesJouees) {
            List<Carte> cartesDeCetteCouleur = mainTmp.get(carte.getCouleur());
            if (cartesDeCetteCouleur != null) cartesDeCetteCouleur.remove(carte);
        }

        float mainScore = calculerScoreAtout(mainTmp) +
        calculerScoreMaitresses(mainTmp) +
        calculerBonusBelote(mainTmp) +
        calculerBonusLongueEtCoupe(mainTmp);

        System.out.println("le score de la main est de: "+mainScore);
        System.out.println("le score du jeu est de: "+globalSum);

        return mainScore + globalSum;
    }


    // Méthode d'inferance qui selon un plis va esayer de recalculer la main d'un jouer
    // argument 1 le plis avant que le joueur ne joue
    // argument 2 la carte joué par le joueur
    public static void inference(Plis before, Carte carte, Joueur j) {
        Carte asked = before.getPlis()[0];

        // Si le joueur est le 1er a jouer on ne déduis rien des règles
        if (asked != null) {
            // Si le joueur ne joue pas la couleur demandé c'est qu'il n'en a pas
            if (! asked.getCouleur().equals(carte.getCouleur())) {
                removeAllCardsOfColor(asked.getCouleur(), j.noPlayer);

                // Si le joueur coupe
                if (carte.getCouleur().getIsAtout())
                    // Si il sous coupe -> il a pas mieux que l'atout demandé
                    if (before.getPowerfullCard().compareTo(carte) > 0)
                        cuteHigherCards(asked, j.noPlayer);

                // Si le joueur ne coupe pas et n'a pas le pli -> pas d'atout en plus
                else if (!before.isForPlayer(j)) removeAllCardsOfColor(colorAtout, j.noPlayer);
            }
        }
        // On enlève la proba de jouer la carte qui vient d'etre joué de tout les joueurs
        for (int i = 0; i < Game.NB_PLAYERS; i++) {
            Map<Couleur, Map<Carte, Float>> proba = cardsProbaPerPlayer.get(i);
            if (proba != null)
                proba.getOrDefault(carte.getCouleur(), Collections.emptyMap()).remove(carte);
        }
    }


    // Enlève toutes les cartes d'une couleur pour un jouer donné
    private static void removeAllCardsOfColor(Couleur couleur, int noPlayer) {
        Map<Couleur, Map<Carte, Float>> proba = cardsProbaPerPlayer.get(noPlayer);

        if (proba != null) {
            Map<Carte, Float> cartes = proba.get(couleur);
            if (cartes != null) {
                // Crée une copie de l'ensemble des clés
                List<Carte> copieCartes = new ArrayList<>(cartes.keySet());
                for (Carte c : copieCartes)
                    distributeProba(c, noPlayer);
            }
        }        

        // On coupe la map de cette couleur pour le joueur noPlayer car il n'en a plus
        cardsProbaPerPlayer.get(noPlayer).remove(couleur);
    }


    // Pour une couleur et un joueur données:
    // enlève les carte qui sont au dessus d'une certaine carte
    // Dans le proba d'un joueur donné
    private static void cuteHigherCards(Carte asked, int noPlayer) {
        Map<Carte, Float> cards = cardsProbaPerPlayer.get(noPlayer).get(asked.getCouleur());

        if (cards == null) return;

        for (Type t : Type.values()) {
            Carte c = new Carte(asked.getCouleur(), t);
            if (asked.compareTo(c) <= 0) distributeProba(c, noPlayer);
        }
    }


    // On enlève une carte du jeu d'un joueur et on distribut cette proba équitablement
    // Avec les joueurs qui ont encore des chances d'avoir cette carte
    private static void distributeProba(Carte carte, int noPlayer) {
        // On doit répartir les proba avec les autres joueurs !
        // 1. Récupère la proba de la carte à enlever
        Float cardProba = cardsProbaPerPlayer.get(noPlayer).get(carte.getCouleur()).get(carte);

        // 2. Calcul la proba a add à chaque joueur qui possède cette carte
        Float additionalProba = cardProba / nbPossibleCardHowner(carte, noPlayer);

        // 3. On l'ajoute a chaque carte joueur
        for (int i = 0; i < Game.NB_PLAYERS; i++) {
            if (i != noPlayer) {
                Map<Couleur, Map<Carte, Float>> proba = cardsProbaPerPlayer.get(i);

                if (proba != null) {
                    Map<Carte, Float> probaColor = proba.get(carte.getCouleur());

                    if (probaColor != null) {
                        Float initialProba = probaColor.get(carte);
                        if (initialProba != null)
                            probaColor.put(carte, initialProba+additionalProba);
                    }
                }
            }
        }
        // Enlève la carte dans les proba du joueur qui vient de la jouer
        cardsProbaPerPlayer.get(noPlayer).get(carte.getCouleur()).remove(carte);
    }


    // Donne le nombre de joueur qui on encore une proba d'avoir la carte
       private static int nbPossibleCardHowner(Carte carte, int noPlayer) {
        int cpt = 0;

        for (int i = 0; i < Game.NB_PLAYERS; i++) {
            if (i != noPlayer) {
                Map<Couleur, Map<Carte, Float>> proba = cardsProbaPerPlayer.get(i);
                if (proba != null) {
                    Map<Carte, Float> probaColor = proba.get(carte.getCouleur());
                    if ((probaColor != null) && (probaColor.get(carte) != null)) cpt++;
                }
            }
        }
        return cpt;
    }

    protected Carte exceptedMiniMaxAlphaBeta(Plis plis, int noCurrentPlayeur, int deepth) {
        return Rules.playable(plis, this).get(0);
    }
}


/**
 * Classe représentant un bot débutant.
 */
class BotDebutant extends Bot {


    public BotDebutant(String nom) {
        super(nom);
    }

    @Override
    public Paquet.Carte jouer(Plis plis) {
        final int DEEPTH = 2;   // Profondeur dans l'arbre de recherche

        System.out.println("\nJeu du bot Débutant: "+ main);
        System.out.println("\n"+getNom() + " (Débutant) joue, il a le choix avec "+ Rules.playable(plis, this));

        // L'IA donne une carte
        Carte carte = exceptedMiniMax(plis, noPlayer, DEEPTH);

        System.out.println("carte joué: "+carte);

        // L'ajoute dans le plis
        plis.addCard(this, carte);

        // L'enlève de la main
        removeCarte(carte);

        return carte;
    }
}

/**
 * Classe représentant un bot intermédiaire.
 */
class BotMoyen extends Bot {
    public BotMoyen(String nom) {
        super(nom);
    }

    @Override
    public Paquet.Carte jouer(Plis plis) {
        final int DEEPTH = 2;   // Profondeur dans l'arbre de recherche

        System.out.println("\nJeu du bot Débutant: "+ main);
        System.out.println("\n"+getNom() + " (Débutant) joue, il a le choix avec "+ Rules.playable(plis, this));

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

/**
 * Classe représentant un bot expert.
 */
class BotExpert extends Bot {
    public BotExpert(String nom) {
        super(nom);
    }

    @Override
    public Paquet.Carte jouer(Plis plis) {
        final int DEEPTH = 5;   // Profondeur dans l'arbre de recherche

        System.out.println("\nJeu du bot Débutant: "+ main);
        System.out.println("\n"+getNom() + " (Débutant) joue, il a le choix avec "+ Rules.playable(plis, this));

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