import chisel3._
import chisel3.util._


class Counter(max_count: Int) extends Module{
    val io = IO(new Bundle{
        val enable  = Input(Bool())
        val count   = Output(UInt(log2Ceil(max_count).W))
        val pulse = Output(Bool())
    })

    val reg_count = RegInit(0.U(log2Ceil(max_count).W))
    when(io.enable){
        reg_count := Mux(reg_count===max_count.U, 0.U, reg_count+1.U)
    }
    io.count := reg_count
    io.pulse := reg_count === max_count.U
}

class SevenSegment extends Module{
    val io = IO(new Bundle{
        val digit   = Input(UInt(4.W))
        val segment = Output(UInt(7.W))
    })
    /* Seven segment names
    *    --a--
    *   |     |
    *   f     b
    *   |     |
    *    --g--
    *   |     |
    *   e     c
    *   |     |
    *    --d--
    */
    val reg_segment = RegInit(0.U(7.W))
    switch(io.digit){
        //                         abcdefg
        is(0.U)  {reg_segment := "b1111110".U}  // 0
        is(1.U)  {reg_segment := "b0110000".U}  // 1
        is(2.U)  {reg_segment := "b1101101".U}  // 2
        is(3.U)  {reg_segment := "b1111001".U}  // 3
        is(4.U)  {reg_segment := "b0110011".U}  // 4
        is(5.U)  {reg_segment := "b1011011".U}  // 5
        is(6.U)  {reg_segment := "b1011111".U}  // 6
        is(7.U)  {reg_segment := "b1110000".U}  // 7
        is(8.U)  {reg_segment := "b1111111".U}  // 8
        is(9.U)  {reg_segment := "b1111011".U}  // 9
        is(10.U) {reg_segment := "b1110111".U}  // A
        is(11.U) {reg_segment := "b0011111".U}  // b
        is(12.U) {reg_segment := "b1001110".U}  // C
        is(13.U) {reg_segment := "b0111101".U}  // d
        is(14.U) {reg_segment := "b1001111".U}  // E
        is(15.U) {reg_segment := "b1000111".U}  // F
    }
    io.segment := ~reg_segment
}

class Top extends Module {
    val io = IO(new Bundle{
        val segment = Output(UInt(7.W))
    })
    val tick_counter  = Module(new Counter(25000000))
    val digit_counter = Module(new Counter(15))
    val seven_segment = Module(new SevenSegment)
    
    // implementation without bug
    //tick_counter.io.enable := true.B 
    
    // implementation with bug
    tick_counter.io.enable := ~tick_counter.io.pulse

    digit_counter.io.enable := tick_counter.io.pulse
    seven_segment.io.digit  := digit_counter.io.count
    io.segment              := seven_segment.io.segment
}

class GoBoardTop extends RawModule {
    val io = IO(new Bundle{
        val clock   = Input(Clock())
        val segment = Output(UInt(7.W))
    })
    // the GoBoard has no reset
    withClockAndReset(io.clock, false.B){
        val top = Module(new Top)
        io.segment := top.io.segment
    }
}

object Main extends App{
    (new chisel3.stage.ChiselStage).emitVerilog(new GoBoardTop, Array("--target-dir", "build/artifacts/netlist/"))
}