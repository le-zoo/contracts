package fr.lezoo.contracts.contract.classic;

import fr.lezoo.contracts.Contracts;
import fr.lezoo.contracts.contract.ContractState;
import fr.lezoo.contracts.contract.ContractType;
import fr.lezoo.contracts.contract.PaymentInfo;
import fr.lezoo.contracts.contract.PaymentType;
import fr.lezoo.contracts.utils.ContractsUtils;
import fr.lezoo.contracts.utils.message.Message;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class KillContract extends ClassicContract {
    private UUID playerToKill;


    public KillContract(ConfigurationSection section) {
        super(section);
        playerToKill = UUID.fromString(section.getString("player-to-kill"));
    }


    public KillContract(UUID employer) {
        super(employer);
        //We register the new parameter to set
        addParameter("player-to-kill", (p, str) -> {
                    if (Contracts.plugin.playerManager.has(str)) {
                        playerToKill = Contracts.plugin.playerManager.get(str);
                        filledParameters.put("player-to-kill",str);
                    }
                    else
                        Message.NOT_VALID_PLAYER.format("input",str).send(p);
                }
        );
    }



    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        LivingEntity killer = e.getEntity().getKiller();
        if (killer instanceof Player) {
            Player killingPlayer = (Player) killer;
            if (killingPlayer.equals(Bukkit.getPlayer(employee)) && e.getEntity().equals(Bukkit.getPlayer(playerToKill))) {
                changeContractState(ContractState.FULFILLED);
            }
        }
    }

    @Override
    public void save(FileConfiguration config) {
        super.save(config);
        String str = contractId.toString();
        //Very important to set the type in the yml
        config.set(str+".type", ContractType.KILL.toString());
        config.set(str + ".player-to-kill", playerToKill.toString());
    }
}
