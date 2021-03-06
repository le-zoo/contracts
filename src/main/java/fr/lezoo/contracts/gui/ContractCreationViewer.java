package fr.lezoo.contracts.gui;

import fr.lezoo.contracts.Contracts;
import fr.lezoo.contracts.contract.Contract;
import fr.lezoo.contracts.contract.ContractType;
import fr.lezoo.contracts.gui.objects.EditableInventory;
import fr.lezoo.contracts.gui.objects.GeneratedInventory;
import fr.lezoo.contracts.gui.objects.item.InventoryItem;
import fr.lezoo.contracts.gui.objects.item.Placeholders;
import fr.lezoo.contracts.gui.objects.item.SimpleItem;
import fr.lezoo.contracts.manager.InventoryManager;
import fr.lezoo.contracts.player.PlayerData;
import fr.lezoo.contracts.utils.ContractsUtils;
import fr.lezoo.contracts.utils.message.Message;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ContractCreationViewer extends EditableInventory {
    public ContractCreationViewer() {
        super("contract-creation");
    }

    @Override
    public InventoryItem loadItem(String function, ConfigurationSection config) {
        if (function.equals("go-back"))
            return new GoBackItem(config);
        if(function.equals("create"))
            return new CreateItem(config);
        if(function.equals("parameter"))
            return new ParameterItem(config);
        return null;
    }

    public ContractCreationInventory newInventory(PlayerData playerData, ContractType contractType) {


        return new ContractCreationInventory(playerData, this, contractType);
    }


    public class GoBackItem extends SimpleItem<ContractCreationInventory> {

        public GoBackItem(ConfigurationSection config) {
            super(config);
        }
    }

    public class CreateItem extends InventoryItem<ContractCreationInventory> {

        public CreateItem(ConfigurationSection config) {
            super(config);
        }

        @Override
        public Placeholders getPlaceholders(ContractCreationInventory inv, int n) {
            return new Placeholders();
        }
    }


    public class ParameterItem extends InventoryItem<ContractCreationInventory> {
        private final FilledParameter filledParameter;
        private final ParameterToFill parameterToFill;


        public ParameterItem(ConfigurationSection config) {
            super(config);
            ConfigurationSection filledParameterSection = config.getConfigurationSection("filled");
            ConfigurationSection parameterToFillSection = config.getConfigurationSection("to-fill");
            Validate.notNull(filledParameterSection, "Couldn't load filled for parameters in contract-creation.yml");
            Validate.notNull(parameterToFillSection, "Couldn't load to-fill for parameters in contract-creation.yml");
            filledParameter = new FilledParameter(this, filledParameterSection);
            parameterToFill = new ParameterToFill(this, parameterToFillSection);
        }

        @Override
        public ItemStack getDisplayedItem(ContractCreationInventory inv, int n) {
            if (inv.parametersList.size() <= n) {
                return new ItemStack(Material.AIR);
            }
            String parameterId = inv.parametersList.get(n);

            ItemStack item;
            if (inv.contract.hasParameter(parameterId))
                item = filledParameter.getDisplayedItem(inv, n);
            else
                item = parameterToFill.getDisplayedItem(inv, n);
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(new NamespacedKey(Contracts.plugin, "parameter"), PersistentDataType.STRING, parameterId);
            item.setItemMeta(meta);
            return item;
        }

        @Override
        public boolean hasDifferentDisplay() {
            return true;
        }

        @Override
        public Placeholders getPlaceholders(ContractCreationInventory inv, int n) {
            Placeholders holders = new Placeholders();
            holders.register("name",ContractsUtils.chatName(inv.parametersList.get(n)));
            return holders;
        }
    }

    public class FilledParameter extends InventoryItem<ContractCreationInventory> {

        public FilledParameter(InventoryItem parent, ConfigurationSection config) {
            super(parent, config);
        }

        @Override
        public Placeholders getPlaceholders(ContractCreationInventory inv, int n) {
            Placeholders placeholders = parent.getPlaceholders(inv, n);
            placeholders.register("value", inv.contract.getFilledParameter(inv.parametersList.get(n)));
            return placeholders;
        }
    }

    public class ParameterToFill extends InventoryItem<ContractCreationInventory> {

        public ParameterToFill(InventoryItem parent, ConfigurationSection config) {
            super(parent, config);
        }

        @Override
        public Placeholders getPlaceholders(ContractCreationInventory inv, int n) {
            return parent.getPlaceholders(inv, n);
        }
    }


    public class ContractCreationInventory extends GeneratedInventory {
        private ContractType contractType;
        private Contract contract;
        private List<String> parametersList;


        public ContractCreationInventory(PlayerData playerData, EditableInventory editable, ContractType contractType) {
            super(playerData, editable);
            this.contractType = contractType;
            //We load the contract and enable the modification of its parameters
            contract = contractType.provide(playerData.getUuid());
            parametersList = contract.getParametersList();
        }

        @Override
        public String applyNamePlaceholders(String str) {
            return ContractsUtils.applyColorCode(str.replace("{type}", ContractsUtils.chatName(contractType.toString())));
        }

        @Override
        public void whenClicked(InventoryClickEvent event, InventoryItem item) {
            if (item instanceof GoBackItem) {
                InventoryManager.CONTRACT_TYPE.newInventory(playerData, ContractTypeViewer.InventoryToOpenType.CREATION_VIEWER).open();
            }
            if (item instanceof CreateItem) {
                if (!contract.hasAllParameters()) {
                    Message.MISSING_CONTRACT_PARAMETER.format().send(player);
                } else {
                    //Create the contract and close the inventory
                    player.getOpenInventory().close();
                    contract.createContract();
                }
            }
            if (item instanceof ParameterItem) {
                String parameter = event.getCurrentItem().getItemMeta().getPersistentDataContainer()
                        .get(new NamespacedKey(Contracts.plugin, "parameter"), PersistentDataType.STRING);
                contract.openChatInput(parameter, playerData, this);
            }
        }

        @Override
        public void whenClosed(InventoryCloseEvent event) {

        }
    }
}
