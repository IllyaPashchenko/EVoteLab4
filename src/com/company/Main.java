package com.company;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        ArrayList<Citizen> people = new ArrayList<>();
        ArrayList<byte[]> votes = new ArrayList<>();

        people.add(new Citizen("Illya", "Pashchenko", 1));
        people.add(new Citizen("Vasyl", "Pupkin", 2));
        people.add(new Citizen("Grigoriy", "Vasilchenko", 3));
        people.add(new Citizen("Fedor", "Soloviy", 4));
        people.add(new Citizen("Denis", "Vilnyj", 5));

        for (Citizen guy : people) {
            guy.setAllPeople(people);
            guy.createBiggerKeys(people.size());
        }

        for (Citizen guy : people) {
            byte[] vote = guy.createVote();
            votes.add(vote);
        }

        people.get(people.size()-1).doDecryption(votes);
    }
}
