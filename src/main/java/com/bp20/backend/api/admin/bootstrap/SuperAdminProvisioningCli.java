package com.bp20.backend.api.admin.bootstrap;

import com.bp20.backend.BackendApplication;
import com.bp20.backend.api.user.domain.User;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.util.Arrays;

public final class SuperAdminProvisioningCli {
    private SuperAdminProvisioningCli() {
    }

    public static void main(String[] args) throws Exception {
        CliOptions options = CliOptions.parse(args);
        String password = readPasswordTwice();

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off")
                .run()) {
            User user = context.getBean(SuperAdminProvisioningService.class).provision(
                    options.email(), password, options.name(), options.phoneNumber()
            );
            System.out.printf("Super administrator created: id=%d, email=%s%n", user.getId(), user.getEmail());
        }
    }

    private static String readPasswordTwice() throws Exception {
        Console console = System.console();
        String password;
        String confirmation;
        if (console != null) {
            password = new String(console.readPassword("Super administrator password: "));
            confirmation = new String(console.readPassword("Confirm password: "));
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Super administrator password (input may be visible): ");
            password = reader.readLine();
            System.out.print("Confirm password: ");
            confirmation = reader.readLine();
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        return password;
    }

    private record CliOptions(String email, String name, String phoneNumber) {
        static CliOptions parse(String[] args) {
            String email = option(args, "--email=");
            String name = option(args, "--name=");
            String phoneNumber = option(args, "--phone-number=");
            if (email == null || email.isBlank() || name == null || name.isBlank()) {
                throw new IllegalArgumentException("--email and --name are required.");
            }
            return new CliOptions(email, name, phoneNumber);
        }

        private static String option(String[] args, String prefix) {
            return Arrays.stream(args)
                    .filter(arg -> arg.startsWith(prefix))
                    .map(arg -> arg.substring(prefix.length()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
