package com.company;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class Citizen {
    public UUID id;
    public String firsName;
    public String surname;
    private ArrayList<Citizen> allPeople;
    private int number;
    private ArrayList<byte[]> myMessages;

    private PrivateKey privateKey;
    public PublicKey publicKey;

    private PrivateKey biggerPrivateKey;
    public PublicKey biggerPublicKey;

    public Citizen(String firsName, String surname, int number) {
        this.id = UUID.randomUUID();
        this.firsName = firsName;
        this.surname = surname;
        this.number = number;
        this.myMessages = new ArrayList<>();

        try {
            KeyPairGenerator smallKeyGen = KeyPairGenerator.getInstance("RSA");
            smallKeyGen.initialize(512 * number, new SecureRandom());
            KeyPair smallKeyPair = smallKeyGen.generateKeyPair();
            publicKey = smallKeyPair.getPublic();
            privateKey = smallKeyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Message " + e.getMessage());
        }
    }

    public void createBiggerKeys(int size){
        try {
            KeyPairGenerator bigKeyGen = KeyPairGenerator.getInstance("RSA");
            bigKeyGen.initialize(512 * (number + size), new SecureRandom());
            KeyPair bigKeyPair = bigKeyGen.generateKeyPair();
            this.biggerPublicKey = bigKeyPair.getPublic();
            this.biggerPrivateKey = bigKeyPair.getPrivate();
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("Message " + ex.getMessage());
        }
    }

    public byte[] createVote(){
        byte[] vote = VoteGenerator.getRandomValidVote().getBytes();
        myMessages.add(vote);
        for (Citizen someone : allPeople) {
            vote = doEncryption(someone.getPublicKey(), vote);
            myMessages.add(vote);
        }
        for (Citizen someone : allPeople) {
            vote = doEncryption(someone.getBiggerPublicKey(), vote);
            myMessages.add(vote);
        }
        return vote;
    }

    public byte[] doEncryption(PublicKey key, byte[] message){
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(message);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

    public void doDecryption(ArrayList<byte[]> votes){
        if (votes.size()>allPeople.size()) System.out.println("Something is wrong, there is more votes than people");
        if (!checkVotesContainYourVote(votes)) System.out.println("Vote of " + firsName + " " + surname + " got lost");
        ArrayList<byte[]> decryptedVotes = new ArrayList<>();

        for (byte[] vote : votes) {
            byte[] decryptVote = decryptVote(vote, this.biggerPrivateKey);
            decryptedVotes.add(decryptVote);
        }

        int indexOfSelf = allPeople.indexOf(this);
        if (indexOfSelf != 0) {
            Collections.shuffle(decryptedVotes);
            allPeople.get(indexOfSelf - 1).doDecryption(decryptedVotes);
        } else {
            allPeople.get(allPeople.size()-1).doSigningAndDecryption(decryptedVotes);
        }
    }

    public void doSigningAndDecryption(ArrayList<byte[]> votes) {
        if (votes.size()>allPeople.size()) System.out.println("Something is wrong, there is more votes than people");
        if (!checkVotesContainYourVote(votes)) System.out.println("The vote from " + surname + " got lost");

        ArrayList<byte[]> decryptedVotes = new ArrayList<>();
        ArrayList<byte[]> signs = new ArrayList<>();

        for (byte[] vote : votes) {
            byte[] decryptVote = decryptVote(vote, this.privateKey);
            decryptedVotes.add(decryptVote);

            byte[] sign = createSign(decryptVote);
            signs.add(sign);
        }

        int indexOfSelf = allPeople.indexOf(this);
        if (indexOfSelf != 0) {
            if (!checkSigns(this, decryptedVotes, signs)) System.out.println("Some signs aren't correct");
            allPeople.get(indexOfSelf - 1).doSigningAndDecryption(decryptedVotes);
        } else {
            concludeVoting(decryptedVotes);
        }
    }

    public byte[] createSign(byte[] vote) {
        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey, new SecureRandom());
            signature.update(vote);
            return signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public boolean checkSigns(Citizen signer, ArrayList<byte[]> votes, ArrayList<byte[]> signs) {
        boolean approved = true;
        for (Citizen someone : allPeople){
            for (int i = 0; i < votes.size(); i++) {
                approved = approved && someone.checkSign(signer, votes.get(i), signs.get(i));
            }
        }
        return approved;
    }

    public boolean checkSign(Citizen signer, byte[] vote, byte[] sign) {
        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(signer.getPublicKey());
            signature.update(vote);
            return signature.verify(sign);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public byte[] decryptVote(byte[] vote, PrivateKey key) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(vote);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public boolean checkVotesContainYourVote(ArrayList<byte[]> votes){
        for (byte[] vote : votes) {
            for (byte[] message : myMessages) {
                if (Arrays.equals(vote, message)) return true;
            }
        }
        return false;
    }

    public void concludeVoting(ArrayList<byte[]> votes) {
        String[] validVotes = VoteGenerator.validVotes.clone();
        int[] scores = new int[validVotes.length];

        for (int i = 0; i < validVotes.length; i++) {
            for (byte[] vote : votes) {
                if (Arrays.equals(validVotes[i].getBytes(), vote)) scores[i]++;
            }
        }

        printStandings(validVotes, scores);
    }

    public void printStandings(String[] options, int[] scores) {
        for (int i = 0; i < options.length; i++) {
            System.out.print(options[i] + " : ");
            System.out.println(scores[i]);
        }
    }

    public ArrayList<Citizen> getAllPeople() {
        return allPeople;
    }

    public void setAllPeople(ArrayList<Citizen> allPeople) {
        this.allPeople = allPeople;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PublicKey getBiggerPublicKey() {
        return biggerPublicKey;
    }
}
