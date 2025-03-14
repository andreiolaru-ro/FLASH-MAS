from server import *

def dotesting():
    model_name = "YOLOv8-pedestrians"
    input_data = ML_DIRECTORY_PATH + "input/a31ee6f_ADE_val_00000798.jpg"

    if model_name in models:
        model = models[model_name]
        processed_input = model.process_input(input_data)
        output = model.predict(processed_input)
        result = model.process_output(output)
        response = {'prediction': (result if isinstance(result, list) else [result])}
        ret = response
        log("returned", ret)
        return ret
    else:
        log("Model not found")
        
        
        
if __name__ == '__main__':
    log("testing...")
    dotesting()    