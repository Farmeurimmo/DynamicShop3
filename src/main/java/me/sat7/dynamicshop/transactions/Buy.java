package me.sat7.dynamicshop.transactions;

import java.util.HashMap;

import me.sat7.dynamicshop.events.ShopBuySellEvent;
import me.sat7.dynamicshop.files.CustomConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.jobshook.JobsHook;
import me.sat7.dynamicshop.utilities.ItemsUtil;
import me.sat7.dynamicshop.utilities.LogUtil;
import me.sat7.dynamicshop.utilities.ShopUtil;
import me.sat7.dynamicshop.utilities.SoundUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import static me.sat7.dynamicshop.utilities.LangUtil.n;
import static me.sat7.dynamicshop.utilities.LangUtil.t;

public final class Buy
{
    private Buy()
    {

    }

    // 구매
    public static void buyItemCash(Player player, String shopName, String tradeIdx, ItemStack tempIS, double priceSum, boolean infiniteStock)
    {
        CustomConfig data = ShopUtil.shopConfigFiles.get(shopName);

        Economy econ = DynamicShop.getEconomy();
        double priceBuyOld = Calc.getCurrentPrice(shopName, tradeIdx, true);
        double priceSellOld = DynaShopAPI.getSellPrice(shopName, tempIS);
        int stockOld = data.get().getInt(tradeIdx + ".stock");

        int actualAmount = 0;

        for (int i = 0; i < tempIS.getAmount(); i++)
        {
            if (!infiniteStock && stockOld <= actualAmount + 1)
            {
                break;
            }

            double price = Calc.getCurrentPrice(shopName, tradeIdx, true);

            if (priceSum + price > econ.getBalance(player)) break;

            priceSum += price;

            if (!infiniteStock)
            {
                data.get().set(tradeIdx + ".stock",
                        data.get().getInt(tradeIdx + ".stock") - 1);
            }

            actualAmount++;
        }

        // 실 구매 가능량이 0이다 = 돈이 없다.
        if (actualAmount <= 0)
        {
            player.sendMessage(DynamicShop.dsPrefix + t("MESSAGE.NOT_ENOUGH_MONEY").replace("{bal}", n(econ.getBalance(player))));
            data.get().set(tradeIdx + ".stock", stockOld);
            return;
        }

        // 상점 재고 부족
        if (!infiniteStock && stockOld <= actualAmount)
        {
            player.sendMessage(DynamicShop.dsPrefix + t("MESSAGE.OUT_OF_STOCK"));
            data.get().set(tradeIdx + ".stock", stockOld);
            return;
        }

        // 실 거래부-------
        if (econ.getBalance(player) >= priceSum)
        {
            EconomyResponse r = DynamicShop.getEconomy().withdrawPlayer(player, priceSum);

            if (r.transactionSuccess())
            {
                int leftAmount = actualAmount;
                while (leftAmount > 0)
                {
                    int giveAmount = tempIS.getType().getMaxStackSize();
                    if (giveAmount > leftAmount) giveAmount = leftAmount;

                    ItemStack iStack = new ItemStack(tempIS.getType(), giveAmount);
                    iStack.setItemMeta((ItemMeta) data.get().get(tradeIdx + ".itemStack"));

                    HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(iStack);
                    if (leftOver.size() != 0)
                    {
                        player.sendMessage(DynamicShop.dsPrefix + t("MESSAGE.INVENTORY_FULL"));
                        Location loc = player.getLocation();

                        ItemStack leftStack = new ItemStack(tempIS.getType(), leftOver.get(0).getAmount());
                        leftStack.setItemMeta((ItemMeta) data.get().get(tradeIdx + ".itemStack"));

                        player.getWorld().dropItem(loc, leftStack);
                    }

                    leftAmount -= giveAmount;
                }

                //로그 기록
                LogUtil.addLog(shopName, tempIS.getType().toString(), actualAmount, priceSum, "vault", player.getName());

                String message = DynamicShop.dsPrefix + t("MESSAGE.BUY_SUCCESS")
                        .replace("{amount}", Integer.toString(actualAmount))
                        .replace("{price}", n(r.amount))
                        .replace("{bal}", n(econ.getBalance((player))));

                if(DynamicShop.localeManager == null)
                {
                    message = message.replace("{item}", ItemsUtil.getBeautifiedName(tempIS.getType()));
                    player.sendMessage(message);
                }
                else
                {
                    message = message.replace("{item}", "<item>");
                    DynamicShop.localeManager.sendMessage(player, message, tempIS.getType(), (short)0, null);
                }

                SoundUtil.playerSoundEffect(player, "buy");

                if (data.get().contains("Options.Balance"))
                {
                    ShopUtil.addShopBalance(shopName, priceSum);
                }

                DynaShopAPI.openItemTradeGui(player, shopName, tradeIdx);
                data.save();

                ShopBuySellEvent event = new ShopBuySellEvent(true, priceBuyOld, Calc.getCurrentPrice(shopName, tradeIdx, true), priceSellOld, DynaShopAPI.getSellPrice(shopName, tempIS), stockOld, DynaShopAPI.getStock(shopName, tempIS), DynaShopAPI.getMedian(shopName, tempIS), shopName, tempIS, player);
                Bukkit.getPluginManager().callEvent(event);
            } else
            {
                player.sendMessage(String.format("An error occured: %s", r.errorMessage));
            }
        } else
        {
            player.sendMessage(DynamicShop.dsPrefix + t("MESSAGE.NOT_ENOUGH_MONEY").replace("{bal}", n(econ.getBalance(player))));
        }
    }

    // 구매 jp
    public static void buyItemJobPoint(Player player, String shopName, String tradeIdx, ItemStack tempIS, double priceSum, boolean infiniteStock)
    {
        CustomConfig data = ShopUtil.shopConfigFiles.get(shopName);

        int actualAmount = 0;
        int stockOld = data.get().getInt(tradeIdx + ".stock");
        double priceBuyOld = Calc.getCurrentPrice(shopName, tradeIdx, true);
        double priceSellOld = DynaShopAPI.getSellPrice(shopName, tempIS);

        for (int i = 0; i < tempIS.getAmount(); i++)
        {
            if (!infiniteStock && stockOld <= actualAmount + 1)
            {
                break;
            }

            double price = Calc.getCurrentPrice(shopName, tradeIdx, true);

            if (priceSum + price > JobsHook.getCurJobPoints(player)) break;

            priceSum += price;

            if (!infiniteStock)
            {
                data.get().set(tradeIdx + ".stock",
                        data.get().getInt(tradeIdx + ".stock") - 1);
            }

            actualAmount++;
        }

        // 실 구매 가능량이 0이다 = 돈이 없다.
        if (actualAmount <= 0)
        {
            player.sendMessage(DynamicShop.dsPrefix + t("MESSAGE.NOT_ENOUGH_POINT").replace("{bal}", n(JobsHook.getCurJobPoints(player))));
            data.get().set(tradeIdx + ".stock", stockOld);
            return;
        }

        // 상점 재고 부족
        if (!infiniteStock && stockOld <= actualAmount)
        {
            player.sendMessage(DynamicShop.dsPrefix + t("MESSAGE.OUT_OF_STOCK"));
            data.get().set(tradeIdx + ".stock", stockOld);
            return;
        }

        // 실 거래부-------
        if (JobsHook.getCurJobPoints(player) >= priceSum)
        {
            if (JobsHook.addJobsPoint(player, priceSum * -1))
            {
                int leftAmount = actualAmount;
                while (leftAmount > 0)
                {
                    int giveAmount = tempIS.getType().getMaxStackSize();
                    if (giveAmount > leftAmount) giveAmount = leftAmount;

                    ItemStack iStack = new ItemStack(tempIS.getType(), giveAmount);
                    iStack.setItemMeta((ItemMeta) data.get().get(tradeIdx + ".itemStack"));

                    HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(iStack);
                    if (leftOver.size() != 0)
                    {
                        player.sendMessage(DynamicShop.dsPrefix + t("MESSAGE.INVENTORY_FULL"));
                        Location loc = player.getLocation();

                        ItemStack leftStack = new ItemStack(tempIS.getType(), leftOver.get(0).getAmount());
                        leftStack.setItemMeta((ItemMeta) data.get().get(tradeIdx + ".itemStack"));

                        player.getWorld().dropItem(loc, leftStack);
                    }

                    leftAmount -= giveAmount;
                }

                //로그 기록
                LogUtil.addLog(shopName, tempIS.getType().toString(), actualAmount, priceSum, "jobpoint", player.getName());

                String message = DynamicShop.dsPrefix + t("MESSAGE.BUY_SUCCESS_JP")
                        .replace("{amount}", Integer.toString(actualAmount))
                        .replace("{price}", n(priceSum))
                        .replace("{bal}", n(JobsHook.getCurJobPoints((player))));

                if(DynamicShop.localeManager == null)
                {
                    message = message.replace("{item}", ItemsUtil.getBeautifiedName(tempIS.getType()));
                    player.sendMessage(message);
                }
                else
                {
                    message = message.replace("{item}", "<item>");
                    DynamicShop.localeManager.sendMessage(player, message, tempIS.getType(), (short)0, null);
                }

                SoundUtil.playerSoundEffect(player, "buy");

                if (data.get().contains("Options.Balance"))
                {
                    ShopUtil.addShopBalance(shopName, priceSum);
                }

                DynaShopAPI.openItemTradeGui(player, shopName, tradeIdx);
                data.save();

                ShopBuySellEvent event = new ShopBuySellEvent(true, priceBuyOld, Calc.getCurrentPrice(shopName, tradeIdx, true), priceSellOld, DynaShopAPI.getSellPrice(shopName, tempIS), stockOld, DynaShopAPI.getStock(shopName, tempIS), DynaShopAPI.getMedian(shopName, tempIS), shopName, tempIS, player);
                Bukkit.getPluginManager().callEvent(event);
            }
        }
    }
}
