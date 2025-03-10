package plugin.oreMining.command;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import plugin.oreMining.Main;
import plugin.oreMining.PlayerScoreData;
import plugin.oreMining.data.ExecutingPlayer;
import plugin.oreMining.mapper.PlayerScoreMapper;
import plugin.oreMining.mapper.data.PlayerScore;


/**
 * 制限時間内に指定した鉱石を採掘して、スコアを獲得するゲームを起動するコマンドです。
 * スコアは鉱石によって変わり、採掘した鉱石の合計によってスコアが変動します。
 * スコアの結果はプレイヤー名、スコア、日時などで保存されます。
 */
public class OreMiningCommand extends BaseCommand implements Listener {

  public static final int GAME_TIME = 30;
  private static final String LIST = "list";
  private final Main main;
  private final PlayerScoreData playerScoreData = new PlayerScoreData();
  private final List<ExecutingPlayer> executingPlayerList = new ArrayList<>();
  private final List<Location> generatedOreLocations = new ArrayList<>();

  public OreMiningCommand(Main main) {
    this.main = main;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label, String[] args) {
    //最初の引数が「list」だったらスコアを一覧表示して処理を終了する
    if (args.length == 1 && LIST.equals(args[0])) {
      sendPlayerScoreList(player);
      return false;
    }

    if (args.length == 0){
      ExecutingPlayer nowPlayerScore = getPlayerScore(player);

      World world = player.getWorld();

      initPlayerStatus(player);

      gamePlay(player, nowPlayerScore, world);
    } else {
      player.sendMessage("実行できません。ゲームを実行する場合は引数なし。"
          + "スコアを表示する場合はlistを入力してください。");
    }
    return true;
  }

  @Override
  public boolean onExecuteNPCPlayerCommand(CommandSender sender, Command command, String label, String[] args) {
    return false;
  }

  /**
   * 現在登録されているスコアの一覧をメッセージに送る。
   *
   * @param player　プレイヤー
   */
  private void sendPlayerScoreList(Player player) {
    List<PlayerScore> playerScoreList = playerScoreData.selectList();
    for (PlayerScore playerScore : playerScoreList) {
      player.sendMessage(playerScore.getId() + " | "
          + playerScore.getPlayerName() + " | "
          + playerScore.getScore() + " | "
          + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
  }

  /**
   *出現した鉱石を採掘した時にスコアを加算する。スコアは鉱石の種類によって変動し、ゲーム終了後に出現した鉱石は削除される。
   * @param e アイテムを採掘した際に発生するイベント
   */
  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Player player = e.getPlayer();
    Block block = e.getBlock();
    Material blockType = block.getType();


   boolean isSpawnOre = generatedOreLocations.stream()
       .anyMatch(loc -> loc.getBlock().getType().equals(blockType));

    if (Objects.isNull(player) || !isSpawnOre) {
      return;
    }

    executingPlayerList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {
          int oreScore = getOreScore(blockType);

          // スコアを加算
          p.setScore(p.getScore() + oreScore);
          player.sendMessage("鉱石を採掘した!" + blockType.name() + " で " + oreScore + " 点獲得。 現在のスコアは" + p.getScore() + "点！");
          // 破壊された鉱石をリストから削除
          generatedOreLocations.removeIf(loc -> loc.equals(block.getLocation()));
        });
  }

  /**
   * ゲーム開始時のプレイヤーステータスを初期化します
   * 体力と空腹値を最大にして、ネザライトのピッケルを装備する
   * @param player　コマンドを実行したプレイヤー
   */
  private static void initPlayerStatus(Player player) {
    //体力と空腹値を最大にする
    player.setHealth(20);
    player.setFoodLevel(20);

    // ネザライトのピッケルと松明を装備させる
    PlayerInventory inventory = player.getInventory();
    inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_PICKAXE));
    inventory.setItemInOffHand(new ItemStack(Material.TORCH));
    player.sendTitle("ピッケルを渡しました! ",
        "鉱石を採掘して高得点を狙ってください!",0, 30, 0);
  }

  /**
   * コマンドでゲームを実行します。規定の時間内に特定の鉱石を採掘するとスコアが加算されます。合計スコアを時間経過後に表示します。
   * @param player　コマンドを実行したプレイヤー
   * @param nowPlayerScore　実行中のプレイヤースコア情報
   */
  private void gamePlay(Player player, ExecutingPlayer nowPlayerScore, World world) {
    Bukkit.getScheduler().runTaskTimer(main, Runnable -> {

      int timeLeft = nowPlayerScore.getGameTime();
      if (timeLeft <= 5 && timeLeft > 0) {
        player.sendTitle("残り " + timeLeft + " 秒", "", 0, 20, 0);
      }

      if (timeLeft <= 0) {
        Runnable.cancel();
        player.sendTitle("ゲームが終了しました！",
            nowPlayerScore.getPlayerName() + " 合計 " + nowPlayerScore.getScore() + "点",
            0, 200, 0);

        removeGeneratedOres(world);
        removePotionEffect(player);

        playerScoreData.insert( new PlayerScore(nowPlayerScore.getPlayerName(),
            nowPlayerScore.getScore()));

        return;
      }

      if (timeLeft == 30){
        OreSpawnLocation(player, world);
      }
      nowPlayerScore.setGameTime(timeLeft -1);
    },1, 21);
  }

  /**
   * 鉱石の種類ごとにスコアを設定する
   * @param blockType　ブロックの種類
   * @return ブロックのスコア情報
   */
  private  int getOreScore(Material blockType) {
    return switch (blockType) {
      case DIAMOND_ORE -> 70;
      case EMERALD_ORE -> 50;
      case GOLD_ORE -> 40;
      case REDSTONE_ORE -> 30;
      case LAPIS_ORE -> 20;
      case COAL_ORE -> 10;
      case IRON_ORE -> 5;
      case STONE -> 1;
      default -> 0;
    };
  }

  /**
   * 現在実行しているプレイヤーのスコア情報を取得する。
   * @param player　コマンドを実行したプレイヤー
   * @return 現在実行しているプレイヤーのスコア情報
   */
  private ExecutingPlayer getPlayerScore(Player player) {
    ExecutingPlayer executingPlayer = new ExecutingPlayer(player.getName());
    if (executingPlayerList.isEmpty()){
       executingPlayer = addNewPlayer(player);
    } else {
       executingPlayer = executingPlayerList.stream().findFirst()
           .map(ps -> ps.getPlayerName().equals(player.getName())
          ? ps
          : addNewPlayer(player)).orElse(executingPlayer);
    }
    executingPlayer.setGameTime(GAME_TIME);
    executingPlayer.setScore(0);
    removePotionEffect(player);
    return executingPlayer;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   * @param player　コマンドを実行したプレイヤー
   * @return 新規プレイヤー
   */
  private ExecutingPlayer addNewPlayer(Player player) {
    ExecutingPlayer newPlayer = new ExecutingPlayer(player.getName());
    executingPlayerList.add(newPlayer);
    return newPlayer;
  }

  /**
   * プレイヤーの前方に 5×5×5 の範囲でランダムに鉱石ブロックを配置する。
   *
   * <p>プレイヤーの現在の位置を基準に、X 軸と Z 軸に +5 ブロックずらした位置に鉱石を生成する。</p>
   *
   * @param player 鉱石をスポーンさせる基準となるプレイヤー
   * @param world  ブロックを配置するワールド
   */
private void OreSpawnLocation(Player player, World world) {
  Location playerLocation = player.getLocation();
  double x = playerLocation.getX();
  double y = playerLocation.getY();
  double z = playerLocation.getZ();

  // ランダムオブジェクトを作成
  SplittableRandom random = new SplittableRandom();

  // 6×6×6 の範囲で鉱石を生成
  for (int i = 0; i <= 5; i++) {
    for (int j = 0; j <= 5; j++) {
      for (int k = 0; k <= 5; k++) {
        int chance = random.nextInt(100);
        Material oreType = getMaterial(chance);
        System.out.println("生成された鉱石: " + oreType);

        Location oreLocation = new Location(world, x + i + 5, y + j, z + k + 5);
        world.getBlockAt(oreLocation).setType(oreType);

        generatedOreLocations.add(oreLocation);

      }
    }
  }
 }

  /**
   * ゲーム終了後に出現させたブロックを削除します。
   * @param world　出現させたブロックを配置するワールド
   */
  private void removeGeneratedOres(World world) {
    for (Location oreLocation : generatedOreLocations) {
      oreLocation.getWorld().getBlockAt(oreLocation).setType(Material.AIR); // 空気ブロックに変更して削除
    }
    generatedOreLocations.clear(); // リストをクリア
  }

  /**
   * 鉱石ごとの出現確率を設定します。
   * @param chance　出現確率
   * @return 鉱石の種類
   */
  private static Material getMaterial(int chance) {
    Material oreType;
    if (chance < 2) {
      oreType = Material.DIAMOND_ORE;
    } else if (chance < 7) {
      oreType = Material.EMERALD_ORE;
    } else if (chance < 15) {
      oreType = Material.GOLD_ORE;
    } else if (chance < 28) {
      oreType = Material.REDSTONE_ORE;
    } else if (chance < 44) {
      oreType = Material.LAPIS_ORE;
    } else if (chance < 64) {
      oreType = Material.COAL_ORE;
    } else if (chance < 84) {
      oreType = Material.IRON_ORE;
    } else {
      oreType = Material.STONE;
    }
    return oreType;
  }

  /**
   * プレイヤーに設定されている特殊効果を除外します。
   *
   * @param player　コマンドを実行したプレイヤー
   */
  private void removePotionEffect(Player player) {
    player.getActivePotionEffects().stream()
        .map(PotionEffect::getType)
        .forEach(player::removePotionEffect);
  }
}

