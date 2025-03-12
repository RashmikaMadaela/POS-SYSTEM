import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SuperSaverPOS - Team SARS
 * 
 * A Point of Sale (POS) system for the Super-Saving supermarket chain.
 * - Allows cashiers to enter item codes & quantities to generate a bill.
 * - Item details are fetched from a CSV file.
 * - Applies discounts (0-75%) & generates a printable bill.
 * - Supports pending bills and revenue reports.
 * 
 * @author TeamSARS
 * @version 1.1
 * @date 2025-03-12
 */

public class SuperSaverPOS {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            POSSystem pos = new POSSystem("items.csv");
            
            while (true) {
                System.out.println("\n=== SuperSaver POS ===");
                System.out.println("1. New Bill");
                System.out.println("2. Retrieve Pending Bill");
                System.out.println("3. Show All Pending Bills");
                System.out.println("4. Generate Revenue Report");
                System.out.println("5. Exit");
                System.out.print("Select an option: ");
                int option = scanner.nextInt();
                scanner.nextLine();  // Consume newline
                
                if (option == 1) {
                    processNewBill(scanner, pos);
                } else if (option == 2) {
                    retrievePendingBill(scanner, pos);
                } else if (option == 3) {
                    pos.showPendingBills();
                } else if (option == 4) {
                    generateRevenueReport(scanner);
                } else if (option == 5) {
                    System.out.println("Exiting SuperSaver POS. Goodbye!");
                    break;
                } else {
                    System.out.println("Invalid option. Try again.");
                }
            }
        }
    }

    /** Processes a new bill */
    private static void processNewBill(Scanner scanner, POSSystem pos) {
        System.out.print("Enter Cashier Name: ");
        String cashier = scanner.nextLine();
        System.out.print("Enter Branch: ");
        String branch = scanner.nextLine();
        System.out.print("Enter Customer Name (or press Enter for Guest): ");
        String customer = scanner.nextLine();

        Bill bill = new Bill(cashier, branch, customer);

        while (true) {
            System.out.print("Enter Item Code (or type 'done' to finish): ");
            String itemCode = scanner.nextLine();
            if (itemCode.equalsIgnoreCase("done")) break;

            Item item = pos.getItem(itemCode);
            if (item == null) {
                System.out.println("Item not found.");
            } else {
                bill.addItem(item);
                System.out.println(item.itemName + " added to the bill.");
            }
        }

        System.out.println("\n--- Bill Summary ---");
        System.out.println("Total Cost: Rs." + bill.getTotalCost());
        System.out.print("Save bill as pending? (yes/no): ");
        String savePending = scanner.nextLine();

        if (savePending.equalsIgnoreCase("yes")) {
            int billId = pos.savePendingBill(bill);
            System.out.println("Bill saved as pending with ID: " + billId);
        } else {
            bill.generatePDF("Bill_" + System.currentTimeMillis() + ".txt");
            System.out.println("Bill finalized and saved as PDF like TXT.");
        }
    }

    /** Retrieves a pending bill */
    private static void retrievePendingBill(Scanner scanner, POSSystem pos) {
        pos.showPendingBills();
        System.out.print("Enter Bill ID to retrieve: ");
        int billId = scanner.nextInt();
        scanner.nextLine(); // Consume newline
    
        Bill bill = pos.retrievePendingBill(billId);
        if (bill == null) {
            System.out.println("No pending bill found with ID: " + billId);
            return;
        }
    
        System.out.println("\n--- Retrieved Bill Summary ---");
        System.out.println("Total Cost: Rs." + bill.getTotalCost());
    
        while (true) {
            System.out.print("Enter Item Code to add (or type 'done' to finish): ");
            String itemCode = scanner.nextLine();
            if (itemCode.equalsIgnoreCase("done")) break;
    
            Item item = pos.getItem(itemCode);
            if (item == null) {
                System.out.println("Item not found.");
            } else {
                bill.addItem(item);
                System.out.println(item.itemName + " added.");
            }
        }
    
        System.out.println("\nUpdated Total Cost: Rs." + bill.getTotalCost());
        System.out.print("Save updated bill as pending? (yes/no): ");
        String savePending = scanner.nextLine();
    
        if (savePending.equalsIgnoreCase("yes")) {
            int newBillId = pos.savePendingBill(bill);
            System.out.println("Bill saved as pending with ID: " + newBillId);
        } else {
            bill.generatePDF("Bill_" + billId + ".txt");
            System.out.println("Bill finalized and saved as PDF like TXT.");
        }
    }

    /** Generates a revenue report based on finalized bill files */
    private static void generateRevenueReport(Scanner scanner) {
        System.out.print("Enter Start Date (yyyy-MM-dd): ");
        LocalDate startDate = LocalDate.parse(scanner.nextLine());
        System.out.print("Enter End Date (yyyy-MM-dd): ");
        LocalDate endDate = LocalDate.parse(scanner.nextLine());

        File folder = new File(".");
        File[] files = folder.listFiles((dir, name) -> name.startsWith("Bill_") && name.endsWith(".txt"));
        
        double totalRevenue = 0.0;
        
        if (files != null) {
            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String firstLine = reader.readLine();
                    if (firstLine != null && firstLine.contains("Bill Receipt")) {
                        String dateLine = reader.readLine();
                        LocalDate billDate = LocalDate.parse(dateLine.split(": ")[1].split(" ")[0]);
                        
                        if (!billDate.isBefore(startDate) && !billDate.isAfter(endDate)) {
                            String lastLine = "";
                            String currentLine;
                            while ((currentLine = reader.readLine()) != null) {
                                lastLine = currentLine;
                            }
                            totalRevenue += Double.parseDouble(lastLine.split("Rs.")[1]);
                        }
                    }
                } catch (IOException | NumberFormatException e) {
                    System.out.println("Error reading bill file: " + file.getName());
                }
            }
        }
        
        String reportFileName = "Revenue_Report_" + startDate + "_to_" + endDate + ".txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFileName))) {
            writer.println("Super-Saving Supermarket - Revenue Report");
            writer.println("Date Range: " + startDate + " to " + endDate);
            writer.println("Total Revenue: Rs." + totalRevenue);
        } catch (IOException e) {
            System.out.println("Error writing revenue report.");
        }
        
        System.out.println("Revenue Report generated: " + reportFileName);
    }
    
}

/**
 * Represents an item in the supermarket.
 */
class Item {
    String itemCode, itemName, size, manufactureDate, expiryDate, manufacturer;
    double price, discount;

    public Item(String itemCode, String itemName, double price, String size,
                String manufactureDate, String expiryDate, String manufacturer, double discount) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.price = price;
        this.size = size;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.manufacturer = manufacturer;
        this.discount = discount;
    }

    public double getDiscountedPrice() {
        return price * (1 - discount / 100);
    }
}

/**
 * Manages item retrieval, pending bills, and billing operations.
 */
class POSSystem {
    Map<String, Item> inventory = new HashMap<>();
    Map<Integer, Bill> pendingBills = new HashMap<>();
    int pendingBillId = 1001;

    public POSSystem(String filePath) {
        loadItemsFromCSV(filePath);
    }

    private void loadItemsFromCSV(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                String code = data[0], name = data[1], size = data[3], manDate = data[4], expDate = data[5], manufacturer = data[6];
                double price = Double.parseDouble(data[2]), discount = Double.parseDouble(data[7]);

                inventory.put(code, new Item(code, name, price, size, manDate, expDate, manufacturer, discount));
            }
        } catch (IOException e) {
            System.out.println("Error reading CSV file.");
        }
    }

    public Item getItem(String itemCode) {
        return inventory.get(itemCode);
    }

    public int savePendingBill(Bill bill) {
        pendingBills.put(pendingBillId, bill);
        return pendingBillId++;
    }

    public Bill retrievePendingBill(int billId) {
        return pendingBills.remove(billId);
    }

    public void showPendingBills() {
        if (pendingBills.isEmpty()) {
            System.out.println("No pending bills.");
            return;
        }
        System.out.println("--- Pending Bills ---");
        for (int id : pendingBills.keySet()) {
            Bill bill = pendingBills.get(id);
            System.out.println("ID: " + id + " | Customer: " + bill.customerName + " | Total: Rs." + bill.getTotalCost());
        }
    }
}

/**
 * Represents a customer's bill and generates a PDF-like text file.
 */
class Bill {
    String cashierName, branch, customerName;
    List<Item> itemList;
    LocalDateTime dateTime;

    public Bill(String cashierName, String branch, String customerName) {
        this.cashierName = cashierName;
        this.branch = branch;
        this.customerName = (customerName.isEmpty()) ? "Guest" : customerName;
        this.itemList = new ArrayList<>();
        this.dateTime = LocalDateTime.now();
    }

    public void addItem(Item item) {
        itemList.add(item);
    }

    public double getTotalCost() {
        return itemList.stream().mapToDouble(Item::getDiscountedPrice).sum();
    }

    /** Generates a bill as a simple text file (alternative to PDF) */
    public void generatePDF(String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Super-Saving Supermarket - Bill Receipt");
            writer.println("Date & Time: " + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("Cashier: " + cashierName + ", Branch: " + branch + ", Customer: " + customerName);
            writer.println("\nItem Details:");
            writer.println("----------------------------------------------------");
            for (Item item : itemList) {
                writer.printf("%s - Rs.%.2f (Discounted: Rs.%.2f)\n", item.itemName, item.price, item.getDiscountedPrice());
            }
            writer.println("\nTotal Cost: Rs." + getTotalCost());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
