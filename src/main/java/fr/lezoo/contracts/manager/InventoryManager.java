package fr.lezoo.contracts.manager;

import fr.lezoo.contracts.Contracts;
import fr.lezoo.contracts.api.ConfigFile;
import fr.lezoo.contracts.gui.*;
import fr.lezoo.contracts.gui.objects.EditableInventory;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class InventoryManager {
    public static final ContractCreationViewer CONTRACT_CREATION = new ContractCreationViewer();
    public static final ContractMarketViewer CONTRACT_MARKET = new ContractMarketViewer();
    public static final ContractPortfolioViewer CONTRACT_PORTFOLIO = new ContractPortfolioViewer();
    public static final ContractTypeViewer CONTRACT_TYPE = new ContractTypeViewer();
    public static final ReputationViewer REPUTATION = new ReputationViewer();
    public static List<EditableInventory> list = Arrays.asList(REPUTATION, CONTRACT_TYPE, CONTRACT_PORTFOLIO, CONTRACT_MARKET, CONTRACT_CREATION);


    public static void load() {
        list.forEach(inv -> {
            Contracts.plugin.configManager.loadDefaultFile("gui", inv.getId() + ".yml");
            try {
                inv.reload(new ConfigFile("/gui", inv.getId()).getConfig());
            } catch (IllegalArgumentException exception) {
                Contracts.log(Level.WARNING, "Could not load inventory " + inv.getId() + ": " + exception.getMessage());
            }
        });
    }
}
