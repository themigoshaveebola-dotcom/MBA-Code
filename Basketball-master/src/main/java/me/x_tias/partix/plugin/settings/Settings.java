package me.x_tias.partix.plugin.settings;

public class Settings {
    public WinType winType;
    public GameType gameType;
    public WaitType waitType;
    public CompType compType;
    public int playersPerTeam;
    public boolean teamLock;
    public boolean arenaLock;
    public boolean suddenDeath;
    public int periods;
    public GameEffectType gameEffect;
    public boolean reboundMachineEnabled; // NEW FIELD

    public Settings(WinType winType, GameType gameType, WaitType waitType, CompType compType, int playersPerTeam, boolean teamLock, boolean arenaLock, boolean suddenDeath, int periods, GameEffectType effect) {
        this.winType = winType;
        this.gameType = gameType;
        this.waitType = waitType;
        this.compType = compType;
        this.playersPerTeam = playersPerTeam;
        this.teamLock = teamLock;
        this.arenaLock = arenaLock;
        this.suddenDeath = suddenDeath;
        this.periods = periods;
        this.gameEffect = effect;
        this.reboundMachineEnabled = false; // DEFAULT TO OFF
    }

    public Settings copy() {
        Settings copy = new Settings(this.winType, this.gameType, this.waitType, this.compType, this.playersPerTeam, this.teamLock, this.arenaLock, this.suddenDeath, this.periods, this.gameEffect);
        copy.reboundMachineEnabled = this.reboundMachineEnabled; // COPY THE SETTING
        return copy;
    }
}