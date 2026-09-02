package com.cristiannustes.security;

public final class PasswordHashTool {

    private PasswordHashTool() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: ./api hash <password>");
            System.exit(2);
            return;
        }
        System.out.println(new PasswordHasher().hash(args[0]));
    }
}
