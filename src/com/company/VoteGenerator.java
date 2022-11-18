package com.company;

import java.util.Random;

public abstract class VoteGenerator {
    public static String[] validVotes = {"Vote1", "Vote2", "Vote3"};

    public static String getRandomValidVote(){
        int index = new Random().nextInt(validVotes.length);
        return validVotes[index];
    }

    public static byte[] getRandomBytes(int length){
        byte[] array = new byte[length];
        new Random().nextBytes(array);
        return array;
    }
}
