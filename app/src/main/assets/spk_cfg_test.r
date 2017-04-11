use_vad = 0;
vad={
    dnnvad={
        parm={
            TARGERRORMAT = HTK;
            SOURCEFORMAT = WAV;
            SOURCERATE   = 625.0;
            SOURCEKIND   = WAVEFORM;
            TARGETRATE   = 200000.0;
            TARGETKIND   = FBANK_D;
            SAVECOMPRESSED = 0;
            SAVEWITHCRC = 0;
            ZMEANSOURCE = 1;
            WINDOWSIZE = 300000.0;

            USEHAMMING = 1;
            PREEMCOEF = 0.97;
            NUMCHANS = 24;
            ESCALE = 1.0;
            SILFLOOR = 50.0;
            ENORMALISE = 0;
            USEPOWER = 1;

            use_dnn=1;
            dnn={
                use_expand_vector=1;
                padding_frame=12;
                sil_thresh=-0.2234;
                speech_thresh=-0.223;
                win=5;
                use_linear_output=0;
                net_fn=${pwd}/vad/natvad;
                trans_fn=${pwd}/vad/natglobal.transf;
                is_bin=0;
                skip_frame=0;
                cache_size=1;
            };
        };
        sil_thresh=-0.1;
        siltrap=10;
        speechtrap=10;
    };
    use_dnn=0;
    left_margin=15;
    right_margin=15;
    min_speech=0;
    version=wakeup_dnnvad_16k_v0.3;
};

parm={
	SOURCEKIND = WAVEFORM;
	SOURCEFORMAT = WAV;
	SOURCERATE = 625.0;
	TARGETFORMAT = HTK;
	TARGETKIND = MFCC_Z;
	TARGETRATE = 100000.0;
	SAVEWITHCRC = 0;
	WINDOWSIZE = 250000.0;
	USEHAMMING = 1;
	PREEMCOEF = 0.97;
	NUMCHANS = 34;
	NUMCEPS = 30;
	CEPLIFTER = 22;
	ESCALE = 1.0;
	ENORMALISE = 1;
	SILFLOOR = 50.0;
	USEPOWER = 1;
	CEPSCALE = 10;
	feature_basic_cols=30;    
	use_z   = 1;
	use_cmn = 0;
    zmean = {
        #start_min_frame  = 12;
        post_update_frame = 15;
        #smooth           = 1;
        left_seek_frame   = 15;
        #min_flush_frame  = 1;
    };
    use_dnn = 0;
    dnn = {
        win                  = 5;
        use_blas             = 0;
        skip_frame           = 0;
        cache_size           = 1;
        use_linear_output    = 0;
        min_flush_frame      = 0;
        max_w                = 2048;
        max_b                = 512;
        data_type            = 1;
        use_fix_float        = 1;
        use_layer0_fix_float = 1;
        use_fix_trans_matrix = 1;
        net_fn               = ${pwd}/dnn/nnet.fix.bin;
        trans_fn             = ${pwd}/dnn/transf.bin;
        use_fix_res          = 1;
        use_lazy_out         = 0;
        is_bin               = 1;
    };
};

vprint = {
	ubm_fn = /sdcard/voiceprint/final.dubm.txt;
	t_fn = /sdcard/voiceprint/final.ie;
};

# -s svm_type
# -t kernel_type
# -d degree
# -g gamma
# -r coef0
# -n nu
# -m cache_size
# -c C
# -e eps
# -p p
# -h shrinking
# -b probability
svm_param = {
    svm_type = 0;
    kernel_type = 0;
    degree = 3;
    gamma = 0;
    coef0 = 0;
    cache_size = 100;
    C =1;
    eps = 0.001;
    p = 0.1;
    shrinking = 1;
    probability = 1;
    nr_weight = 0;

    scale_lower = -1;
    scale_upper = 1;
}

#svm2_param = {
#    svm_type = 0;
#    kernel_type = 0;
#    degree = 3;
#    gamma = 0;
#    coef0 = 0;
#    cache_size = 100;
#    C =1;
#    eps = 0.001;
#    p = 0.1;
#    shrinking = 1;
#    probability = 1;
#    nr_weight = 0;
#
#    scale_lower = -1;
#    scale_upper = 1;
#}

svm_res_fn = /sdcard/voiceprint/svm_res.txt;
svm_scale_res_fn = /sdcard/voiceprint/svm_scale.txt;
class_model_fn = /sdcard/voiceprint/class_res.txt;
debug_fn = /sdcard/voiceprint/debug.out;
train_ivct_fn =	//sdcard/voiceprint/train_ivct.txt;
spk_strategy = 0;
doing_type = 1;
use_scale = 1;
use_norm = 0;
debug_mode = 0;
